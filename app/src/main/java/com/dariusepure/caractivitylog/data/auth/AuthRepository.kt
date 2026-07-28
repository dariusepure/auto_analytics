package com.dariusepure.caractivitylog.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.ActionCodeSettings
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
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) {
    private val sharedPrefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    private val _isGuestMode = MutableStateFlow(sharedPrefs.getBoolean("is_guest_mode", false))
    
    private val TAG = "AuthRepository"

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

    fun getUserId(): String? {
        return if (_isGuestMode.value) "guest_user" else firebaseAuth.currentUser?.uid
    }

    fun setGuestMode(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("is_guest_mode", enabled).apply()
        _isGuestMode.value = enabled
    }

    suspend fun isUsernameAvailable(username: String): Boolean {
        if (username.isBlank()) return false
        val doc = firestore.collection("usernames").document(username.lowercase()).get().await()
        return !doc.exists()
    }

    suspend fun signUp(email: String, password: String, fullName: String, username: String) {
        val usernameLower = username.lowercase()
        
        // Double check username availability
        if (!isUsernameAvailable(usernameLower)) {
            throw Exception("Username is already taken")
        }

        val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val uid = authResult.user?.uid ?: throw Exception("Failed to get user ID")

        val user = FirestoreUser(
            id = uid,
            fullName = fullName,
            username = username,
            email = email
        )

        // Use a batch to ensure both documents are created
        firestore.runBatch { batch ->
            batch.set(firestore.collection("users").document(uid), user)
            batch.set(firestore.collection("usernames").document(usernameLower), mapOf("uid" to uid))
        }.await()
    }

    suspend fun signIn(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .await()
    }

    suspend fun signInWithGoogle(context: Context) {
        val webClientId = BuildConfig.WEB_CLIENT_ID
        Log.d(TAG, "Starting Google Sign-In with WEB_CLIENT_ID: '$webClientId'")
        
        if (webClientId.isBlank()) {
            Log.e(TAG, "WEB_CLIENT_ID is empty! Google Sign-In will fail.")
            throw IllegalStateException("Web Client ID is not configured in local.properties")
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val credentialManager = CredentialManager.create(context)
            Log.d(TAG, "Calling getCredential...")
            val response = credentialManager.getCredential(context, request)
            val credential = response.credential

            Log.d(TAG, "Received credential type: ${credential.type}")

            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                Log.d(TAG, "ID Token obtained successfully")
                
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                firebaseAuth.signInWithCredential(firebaseCredential).await()
                Log.d(TAG, "Firebase sign-in successful")
            } else {
                Log.e(TAG, "Unexpected credential type: ${credential.type}")
                throw IllegalStateException("Unexpected credential type: ${credential.type}")
            }
        } catch (e: NoCredentialException) {
            Log.e(TAG, "No credentials found. This usually means the SHA-1 is not registered in Firebase or no Google account is available on device.", e)
            throw Exception("No Google accounts found or configuration issue (check SHA-1 in Firebase).")
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential Manager error: ${e.type}", e)
            throw Exception("Google Sign-In failed: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during Google Sign-In", e)
            throw e
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
    }

    suspend fun sendPasswordResetEmail(email: String) {
        val actionCodeSettings = ActionCodeSettings.newBuilder()
            .setUrl("https://caractivitylog.page.link/reset")
            .setHandleCodeInApp(true)
            .setAndroidPackageName(
                "com.dariusepure.caractivitylog",
                true,
                "1"
            )
            .build()
        
        firebaseAuth.sendPasswordResetEmail(email, actionCodeSettings).await()
    }

    suspend fun confirmPasswordReset(oobCode: String, newPassword: String) {
        firebaseAuth.confirmPasswordReset(oobCode, newPassword).await()
    }
}