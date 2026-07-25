package com.dariusepure.caractivitylog.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.appcheck.FirebaseAppCheck
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.dariusepure.caractivitylog.BuildConfig
import com.dariusepure.caractivitylog.domain.User
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) {
    private val sharedPrefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    private val _isGuestMode = MutableStateFlow(sharedPrefs.getBoolean("is_guest_mode", false))
    private val _refreshVerification = MutableSharedFlow<Unit>(replay = 1)

    val signedIn: Flow<Boolean> = combine(
        callbackFlow {
            val listener = FirebaseAuth.AuthStateListener { auth ->
                trySend(auth.currentUser != null)
            }
            firebaseAuth.addAuthStateListener(listener)
            awaitClose { firebaseAuth.removeAuthStateListener(listener) }
        },
        _isGuestMode
    ) { firebaseSignedIn, guestMode ->
        firebaseSignedIn || guestMode
    }

    val isEmailVerified: Flow<Boolean> = combine(
        callbackFlow {
            val listener = FirebaseAuth.AuthStateListener {
                trySend(Unit)
            }
            firebaseAuth.addAuthStateListener(listener)
            awaitClose { firebaseAuth.removeAuthStateListener(listener) }
        },
        _refreshVerification.onStart { emit(Unit) }
    ) { _, _ ->
        firebaseAuth.currentUser?.isEmailVerified ?: false
    }

    val isCurrentlySignedIn: Boolean
        get() = firebaseAuth.currentUser != null || _isGuestMode.value

    val isCurrentlyVerified: Boolean
        get() = firebaseAuth.currentUser?.isEmailVerified ?: false

    val isGuestMode: Boolean
        get() = _isGuestMode.value

    fun setGuestMode(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("is_guest_mode", enabled).apply()
        _isGuestMode.value = enabled
    }

    fun getUserId(): String? {
        return firebaseAuth.currentUser?.uid ?: if (_isGuestMode.value) "guest_user" else null
    }

    fun checkVerificationStatus(): Boolean {
        return firebaseAuth.currentUser?.isEmailVerified ?: false
    }

    suspend fun isUsernameAvailable(username: String): Boolean {
        if (username.length < 3) return false
        val doc = firestore.collection("usernames").document(username.lowercase()).get().await()
        return !doc.exists()
    }

    suspend fun signUp(email: String, password: String, name: String, username: String) {
        debugAppCheck()
        val normalizedUsername = username.lowercase().trim()
        android.util.Log.d("AuthRepository", "Starting signUp for $email, username: $normalizedUsername")
        
        // Use a transaction to ensure atomic check-and-set for username uniqueness
        try {
            firestore.runTransaction { transaction ->
                val usernameRef = firestore.collection("usernames").document(normalizedUsername)
                if (transaction.get(usernameRef).exists()) {
                    throw Exception("Username is already taken")
                }
            }.await()
            android.util.Log.d("AuthRepository", "Username $normalizedUsername is available")
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error checking username availability", e)
            if (e.message == "Username is already taken") throw e
        }

        val result = try {
            firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Firebase Auth createUser failed", e)
            throw e
        }
        
        val user = result.user ?: throw Exception("Failed to create user")
        android.util.Log.d("AuthRepository", "Firebase user created: ${user.uid}")

        try {
            val userData = mapOf(
                "uid" to user.uid,
                "name" to name,
                "username" to normalizedUsername,
                "email" to email
            )

            firestore.runTransaction { transaction ->
                val userRef = firestore.collection("users").document(user.uid)
                val usernameRef = firestore.collection("usernames").document(normalizedUsername)
                
                if (transaction.get(usernameRef).exists()) {
                    throw Exception("Username was taken during registration")
                }

                transaction.set(userRef, userData)
                transaction.set(usernameRef, mapOf("uid" to user.uid))
            }.await()
            android.util.Log.d("AuthRepository", "Firestore profile created for ${user.uid}")
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Firestore transaction failed, deleting Auth user", e)
            user.delete().await()
            throw e
        }

        try {
            sendEmailVerification()
            android.util.Log.d("AuthRepository", "Verification email sent")
        } catch (e: Exception) {
            android.util.Log.w("AuthRepository", "Verification email failed but account is created", e)
        }
    }

    suspend fun sendEmailVerification() {
        firebaseAuth.currentUser?.sendEmailVerification()?.await()
    }

    suspend fun reloadUser() {
        firebaseAuth.currentUser?.reload()?.await()
        _refreshVerification.emit(Unit)
    }

    suspend fun signIn(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .await()
    }

    suspend fun signInWithGoogle(context: Context) {
        debugAppCheck()
        // Log the SHA-1 for debugging purposes
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val packageInfo = context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
                )
                val signingInfo = packageInfo.signingInfo
                if (signingInfo != null) {
                    val signatures = if (signingInfo.hasMultipleSigners()) {
                        signingInfo.signingCertificateHistory
                    } else {
                        signingInfo.apkContentsSigners
                    }
                    for (signature in signatures) {
                        val md = java.security.MessageDigest.getInstance("SHA-1")
                        val digest = md.digest(signature.toByteArray())
                        val sha1 = digest.joinToString(":") { "%02x".format(it) }
                        android.util.Log.d("AuthRepository", "SHA-1: $sha1")
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val packageInfo = context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.GET_SIGNATURES
                )
                @Suppress("DEPRECATION")
                packageInfo.signatures?.forEach { signature ->
                    val md = java.security.MessageDigest.getInstance("SHA-1")
                    val digest = md.digest(signature.toByteArray())
                    val sha1 = digest.joinToString(":") { "%02x".format(it) }
                    android.util.Log.d("AuthRepository", "SHA-1: $sha1")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error getting SHA-1", e)
        }

        val clientId = BuildConfig.WEB_CLIENT_ID
        if (clientId.isBlank()) {
            throw IllegalStateException("Google Web Client ID is missing. Please add WEB_CLIENT_ID to local.properties or ensure google-services.json is valid.")
        }

        android.util.Log.d("AuthRepository", "Using WEB_CLIENT_ID: $clientId")

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(clientId)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false) // Disable for now to force the picker
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val credentialManager = CredentialManager.create(context)
            android.util.Log.d("AuthRepository", "Requesting credentials...")
            val response = credentialManager.getCredential(context, request)
            val credential = response.credential
            android.util.Log.d("AuthRepository", "Credential received: ${credential.type}")

            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                firebaseAuth.signInWithCredential(firebaseCredential).await()
                android.util.Log.d("AuthRepository", "Google sign-in successful")
            } else {
                throw IllegalStateException("Unexpected credential type: ${credential.type}")
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Google sign-in error", e)
            throw e
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
    }

    private suspend fun debugAppCheck() {
        try {
            android.util.Log.d("AuthRepository", "Checking App Check status...")
            val tokenResult = FirebaseAppCheck.getInstance().getAppCheckToken(false).await()
            android.util.Log.d("AuthRepository", "App Check Token retrieved successfully: ${tokenResult.token.take(10)}...")
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "App Check token retrieval FAILED. This will likely block Firebase requests.", e)
        }
    }
}
