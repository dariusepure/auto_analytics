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
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import com.dariusepure.caractivitylog.R
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val preferenceRepository: com.dariusepure.caractivitylog.data.prefs.PreferenceRepository
) {
    private val TAG = "AuthRepository"

    companion object {
        const val GUEST_UID = "local_guest_user"
    }

    val signedIn: Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener {
            trySend(firebaseAuth.currentUser != null)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    val userId: Flow<String?> = signedIn.combine(preferenceRepository.isGuestMode) { signedIn, isGuest ->
        if (isGuest) GUEST_UID
        else if (signedIn) firebaseAuth.currentUser?.uid
        else null
    }.distinctUntilChanged()

    val userEmailFlow: Flow<String?> = signedIn.map { 
        if (it) firebaseAuth.currentUser?.email else null 
    }.distinctUntilChanged()

    val isAnonymousFlow: Flow<Boolean> = signedIn.combine(preferenceRepository.isGuestMode) { signedIn, isGuest ->
        isGuest || (signedIn && firebaseAuth.currentUser?.isAnonymous == true)
    }.distinctUntilChanged()

    val isCurrentlySignedIn: Boolean
        get() = firebaseAuth.currentUser != null

    val currentUserEmail: String?
        get() = firebaseAuth.currentUser?.email

    val isAnonymous: Boolean
        get() = firebaseAuth.currentUser?.isAnonymous == true

    val isGuestMode: Flow<Boolean> = preferenceRepository.isGuestMode

    val isCurrentlyGuest: Boolean
        get() = preferenceRepository.isGuestMode.value

    fun getUserId(): String? {
        if (isCurrentlyGuest) return GUEST_UID
        return firebaseAuth.currentUser?.uid
    }

    fun getUserData(uid: String): Flow<com.dariusepure.caractivitylog.domain.User?> = callbackFlow {
        if (uid == GUEST_UID) {
            trySend(com.dariusepure.caractivitylog.domain.User(GUEST_UID, "", "Guest"))
            close()
            return@callbackFlow
        }
        val listener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val user = snapshot?.toObject(FirestoreUser::class.java)?.fromFirebase()
                trySend(user)
            }
        awaitClose { listener.remove() }
    }

    suspend fun signInAnonymously() {
        firebaseAuth.signInAnonymously().await()
    }

    fun continueAsGuest() {
        preferenceRepository.setGuestMode(true)
    }

    suspend fun signUp(email: String, password: String, name: String) {
        val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val uid = authResult.user?.uid ?: throw Exception("Failed to get user ID")

        val user = FirestoreUser(
            id = uid,
            email = email,
            name = name
        )

        firestore.collection("users").document(uid).set(user).await()
    }

    suspend fun signIn(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .await()
    }

    suspend fun signInWithGoogle(context: Context) {
        val webClientId = com.dariusepure.caractivitylog.BuildConfig.WEB_CLIENT_ID.trim()
        Log.d(TAG, "Starting Google Sign-In with WEB_CLIENT_ID: '$webClientId'")

        val activity = findActivity(context) ?: throw IllegalStateException("Context is not an Activity")
        val credentialManager = CredentialManager.create(activity)

        // Resetăm starea cache-ului pentru a elimina erori de tip "Account reauth failed"
        try {
            Log.d(TAG, "Clearing cached credential state...")
            credentialManager.clearCredentialState(androidx.credentials.ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear credential state: ${e.message}")
        }

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

        } catch (e: GetCredentialException) {
            if (e is NoCredentialException || e !is androidx.credentials.exceptions.GetCredentialCancellationException) {
                Log.w(TAG, "Primary flow failed (Type=${e.type}). Launching Legacy Fallback Flow...", e)

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
            } else {
                Log.i(TAG, "User canceled Google Sign-In.")
                throw Exception("Google Sign-In failed: ${e.message}")
            }
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
            val displayName = googleIdTokenCredential.displayName ?: ""

            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user

            if (user != null) {
                // Sync Google profile info to Firestore
                val firestoreUser = FirestoreUser(
                    id = user.uid,
                    email = user.email ?: "",
                    name = if (displayName.isNotEmpty()) displayName else (user.displayName ?: "")
                )
                firestore.collection("users").document(user.uid).set(firestoreUser).await()
            }
            
            Log.d(TAG, "Firebase sign-in and Firestore sync successful")
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

    suspend fun reauthenticate(password: String) {
        val user = firebaseAuth.currentUser ?: throw Exception("No user signed in")
        val email = user.email ?: throw Exception("User has no email")
        val credential = EmailAuthProvider.getCredential(email, password)
        user.reauthenticate(credential).await()
    }

    suspend fun updatePassword(newPassword: String) {
        val user = firebaseAuth.currentUser ?: throw Exception("No user signed in")
        user.updatePassword(newPassword).await()
    }

    suspend fun deleteAccount() {
        val user = firebaseAuth.currentUser ?: throw Exception("No user signed in")
        val uid = user.uid
        
        // 1. Delete Firestore user document
        firestore.collection("users").document(uid).delete().await()
        
        // Note: Sub-collections like 'cars' will remain as orphans unless deleted recursively.
        // For a client-side implementation, we prioritize deleting the Auth account and profile doc.
        
        // 2. Delete Auth account
        user.delete().await()
    }

    fun isPasswordUser(): Boolean {
        return firebaseAuth.currentUser?.providerData?.any { 
            it.providerId == EmailAuthProvider.PROVIDER_ID 
        } ?: false
    }
}
