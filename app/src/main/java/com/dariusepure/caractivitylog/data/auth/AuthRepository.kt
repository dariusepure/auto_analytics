package com.dariusepure.caractivitylog.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.dariusepure.caractivitylog.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    @ApplicationContext private val context: Context
) {
    private val sharedPrefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    private val _isGuestMode = MutableStateFlow(sharedPrefs.getBoolean("is_guest_mode", false))

    val signedIn: Flow<Boolean> = combine(
        callbackFlow {
            val listener = FirebaseAuth.AuthStateListener {
                trySend(firebaseAuth.currentUser != null)
            }
            firebaseAuth.addAuthStateListener(listener)
            awaitClose { firebaseAuth.removeAuthStateListener(listener) }
        },
        _isGuestMode
    ) { firebaseSignedIn, guestMode ->
        firebaseSignedIn || guestMode
    }

    val isCurrentlySignedIn: Boolean
        get() = firebaseAuth.currentUser != null || _isGuestMode.value

    val isGuestMode: Boolean
        get() = _isGuestMode.value

    fun setGuestMode(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("is_guest_mode", enabled).apply()
        _isGuestMode.value = enabled
    }

    fun getUserId(): String? {
        return firebaseAuth.currentUser?.uid ?: if (_isGuestMode.value) "guest_user" else null
    }

    suspend fun signUp(email: String, password: String) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .await()
    }

    suspend fun signIn(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .await()
    }

    suspend fun signInWithGoogle(context: Context) {
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

        android.util.Log.d("AuthRepository", "Using WEB_CLIENT_ID: ${BuildConfig.WEB_CLIENT_ID}")

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(BuildConfig.WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false) // Disable for now to force the picker
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val credentialManager = CredentialManager.create(context)
            val response = credentialManager.getCredential(context, request)
            val credential = response.credential

            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                firebaseAuth.signInWithCredential(firebaseCredential).await()
            } else {
                throw IllegalStateException("Unexpected credential type: ${credential.type}")
            }
        } catch (e: Exception) {
            // Re-throw or handle specifically if needed
            throw e
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
    }
}