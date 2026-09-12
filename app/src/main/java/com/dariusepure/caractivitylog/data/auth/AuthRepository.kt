package com.dariusepure.caractivitylog.data.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.ActionCodeSettings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.dariusepure.caractivitylog.R
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private val TAG = "AuthRepository"

    val signedIn: Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener {
            trySend(firebaseAuth.currentUser != null)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    val isCurrentlySignedIn: Boolean
        get() = firebaseAuth.currentUser != null

    val currentUserEmail: String?
        get() = firebaseAuth.currentUser?.email

    fun getUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }

    suspend fun signUp(email: String, password: String) {
        val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val uid = authResult.user?.uid ?: throw Exception("Failed to get user ID")

        val user = FirestoreUser(
            id = uid,
            email = email
        )

        firestore.collection("users").document(uid).set(user).await()
    }

    suspend fun signIn(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .await()
    }

    suspend fun signInWithGoogle(context: Context) {
        // 💡 REZOLVARE: Introducem direct string-ul din JSON-ul tău (client_type 3) pentru a evita erorile de compilare Gradle
        val webClientId = "://googleusercontent.com"
        Log.d(TAG, "Starting Google Sign-In with HARDCODED WEB_CLIENT_ID: '$webClientId'")

        val activity = findActivity(context) ?: throw IllegalStateException("Context is not an Activity")
        val credentialManager = CredentialManager.create(activity)

        // 1. Încercarea primară: Folosim opțiunea modernă Google Id
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()

        val primaryRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            Log.d(TAG, "Calling getCredential with Primary Flow...")
            val response = credentialManager.getCredential(activity, primaryRequest)
            processCredentialResult(response.credential)

        } catch (e: NoCredentialException) {
            // 2. CATCH & FALLBACK: Dacă noul API eșuează local, forțăm dialogul clasic nativ Google
            Log.w(TAG, "No credentials found in primary flow. Launching Legacy Fallback Flow...", e)

            val legacyGoogleOption = GetSignInWithGoogleOption.Builder(webClientId)
                .build()

            val fallbackRequest = GetCredentialRequest.Builder()
                .addCredentialOption(legacyGoogleOption)
                .build()

            try {
                val response = credentialManager.getCredential(activity, fallbackRequest)
                processCredentialResult(response.credential)
            } catch (fallbackError: GetCredentialException) {
                Log.e(TAG, "Fallback Google Sign-In failed: ${fallbackError.type}", fallbackError)
                throw Exception("Google Sign-In failed: ${fallbackError.message}")
            }

        } catch (e: GetCredentialException) {
            Log.e(TAG, "GetCredentialException: Type=${e.type}, Message=${e.message}")
            throw Exception("Google Sign-In failed: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected Exception: ${e.message}", e)
            throw e
        }
    }

    private suspend fun processCredentialResult(credential: androidx.credentials.Credential) {
        Log.d(TAG, "Received credential type: ${credential.type}")

        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken

            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(firebaseCredential).await()
            Log.d(TAG, "Firebase sign-in successful")
        } else {
            Log.e(TAG, "Unexpected credential type: ${credential.type}")
            throw IllegalStateException("Unexpected credential type: ${credential.type}")
        }
    }

    private fun findActivity(context: Context): Activity? {
        var currentContext = context
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) return currentContext
            currentContext = currentContext.baseContext
        }
        return null
    }

    fun signOut() {
        firebaseAuth.signOut()
    }

    suspend fun sendPasswordResetEmail(email: String) {
        val actionCodeSettings = ActionCodeSettings.newBuilder()
            .setUrl("https://page.link")
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
