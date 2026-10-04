package com.dariusepure.caractivitylog.data.auth

import android.content.Context
import android.util.Log
import android.app.Activity
import android.content.ContextWrapper
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.dariusepure.caractivitylog.BuildConfig
import com.dariusepure.caractivitylog.data.prefs.PreferenceRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.dariusepure.caractivitylog.domain.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val preferenceRepository: PreferenceRepository
) {
    private val TAG = "AuthRepository"

    private val _authEvents = MutableSharedFlow<AuthEvent>(replay = 0)
    val authEvents: SharedFlow<AuthEvent> = _authEvents.asSharedFlow()

    companion object {
        const val GUEST_UID = "local_guest_user"
    }

    val signedIn: Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val isAuthenticated = auth.currentUser != null || preferenceRepository.cachedSignedIn.value
            if (auth.currentUser != null) {
                preferenceRepository.setCachedSignedIn(true)
            }
            trySend(isAuthenticated || preferenceRepository.isGuestMode.value)
        }
        firebaseAuth.addAuthStateListener(listener)
        trySend(firebaseAuth.currentUser != null || preferenceRepository.cachedSignedIn.value || preferenceRepository.isGuestMode.value)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    val initialSignedIn: Boolean
        get() = preferenceRepository.cachedSignedIn.value || preferenceRepository.isGuestMode.value

    val userId: Flow<String?> = signedIn.combine(preferenceRepository.isGuestMode) { signedInUser, isGuest ->
        val currentUser = firebaseAuth.currentUser
        if (signedInUser && currentUser != null) {
            if (isGuest) preferenceRepository.setGuestMode(false)
            currentUser.uid
        } else if (isGuest) GUEST_UID
        else null
    }.distinctUntilChanged()

    val userEmailFlow: Flow<String?> = signedIn.map { 
        if (it) firebaseAuth.currentUser?.email else null 
    }.distinctUntilChanged()

    val isAnonymousFlow: Flow<Boolean> = signedIn.combine(preferenceRepository.isGuestMode) { signedInUser, isGuest ->
        if (signedInUser) false else isGuest
    }.distinctUntilChanged()

    val isCurrentlySignedIn: Boolean
        get() = firebaseAuth.currentUser != null

    val currentUserEmail: String?
        get() = firebaseAuth.currentUser?.email

    val isAnonymous: Boolean
        get() = false

    val isGuestMode: Flow<Boolean> = preferenceRepository.isGuestMode

    val isCurrentlyGuest: Boolean
        get() = !isCurrentlySignedIn && preferenceRepository.isGuestMode.value

    fun getUserId(): String? {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) return currentUser.uid
        if (isCurrentlyGuest) return GUEST_UID
        return null
    }

    suspend fun ensureProfileExists(uid: String, email: String, name: String = "") {
        if (uid.isBlank() || uid == "unknown_uid" || uid == GUEST_UID || uid == "guest") return
        try {
            val userEmail = email.ifBlank { currentUserEmail ?: "" }
            val googleDisplayName = firebaseAuth.currentUser?.displayName
            
            val displayName = name.ifBlank {
                googleDisplayName
                    ?: userEmail.substringBefore("@")
            }.ifBlank { "User" }
            
            val username = userEmail.substringBefore("@").ifBlank { displayName }
            val user = RemoteUser(
                id = uid,
                email = userEmail,
                fullName = displayName,
                username = username
            )
            firestore.collection("users").document(uid).set(user).await()
            Log.d(TAG, "ensureProfileExists: Upserted profile for $uid ($userEmail) with name: $displayName")
        } catch (e: Exception) {
            Log.e(TAG, "ensureProfileExists error for $uid: ${e.message}", e)
        }
    }

    fun getUserData(uid: String): Flow<User?> = flow {
        if (uid == GUEST_UID || uid == "guest") {
            Log.d(TAG, "getUserData: Guest mode, emitting Guest user")
            emit(User(GUEST_UID, "", "Guest"))
            return@flow
        }
        
        try {
            Log.d(TAG, "getUserData: Fetching profile for UID: $uid")
            val snapshot = firestore.collection("users").document(uid).get().await()
            val googleDisplayName = firebaseAuth.currentUser?.displayName
            
            if (snapshot.exists()) {
                val email = snapshot.getString("email") ?: currentUserEmail ?: ""
                val fullName = snapshot.getString("fullName") ?: snapshot.getString("name") ?: snapshot.getString("display_name") ?: snapshot.getString("displayName") ?: ""
                
                val effectiveFullName = if (!googleDisplayName.isNullOrBlank() && (fullName.isBlank() || fullName == email || fullName == email.substringBefore("@"))) {
                    googleDisplayName
                } else {
                    fullName
                }
                
                val username = snapshot.getString("username") ?: ""
                val name = effectiveFullName.ifBlank { username.ifBlank { googleDisplayName?.ifBlank { null } ?: email.substringBefore("@").ifBlank { "User" } } }
                val user = User(uid, email, name)
                Log.d(TAG, "getUserData: Profile found: ${user.name} (${user.email})")
                emit(user)
            } else {
                val email = currentUserEmail ?: ""
                val name = googleDisplayName?.ifBlank { null } ?: email.substringBefore("@").ifBlank { "User" }
                Log.w(TAG, "getUserData: Profile NOT FOUND for $uid. Email: $email. Creating profile now.")
                emit(User(uid, email, name))
                ensureProfileExists(uid, email, name)
            }
        } catch (e: Exception) {
            Log.e(TAG, "getUserData: Error fetching profile for $uid: ${e.message}", e)
            emit(User(uid, currentUserEmail ?: "", "User"))
        }
    }

    fun signInAnonymously() {
    }

    fun continueAsGuest() {
        preferenceRepository.setGuestMode(true)
    }

    suspend fun signUp(email: String, password: String, name: String) {
        preferenceRepository.setGuestMode(false)
        val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val user = authResult.user
        val uid = user?.uid ?: "unknown_uid"
        
        withContext(NonCancellable) {
            if (uid != "unknown_uid") {
                if (name.isNotBlank()) {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()
                    user?.updateProfile(profileUpdates)?.await()
                }
                ensureProfileExists(uid, email, name)
            }
        }
    }

    suspend fun signIn(email: String, password: String) {
        preferenceRepository.setGuestMode(false)
        val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
        val user = authResult.user
        if (user != null) {
            withContext(NonCancellable) {
                ensureProfileExists(user.uid, user.email ?: "", user.displayName ?: "")
            }
        }
    }

    suspend fun signInWithGoogle(context: Context) {
        val webClientId = BuildConfig.WEB_CLIENT_ID.trim()
        Log.d(TAG, "Starting Google Sign-In with WEB_CLIENT_ID: '$webClientId'")

        val activity = findActivity(context) ?: throw IllegalStateException("Context is not an Activity")
        val credentialManager = CredentialManager.create(activity)

        try {
            Log.d(TAG, "Clearing cached credential state...")
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear credential state: ${e.message}")
        }

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
            if (e is NoCredentialException || e !is GetCredentialCancellationException) {
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

    private suspend fun processCredentialResult(credential: Credential) {
        preferenceRepository.setGuestMode(false)
        Log.d(TAG, "Received credential type: ${credential.type}")

        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idTokenString = googleIdTokenCredential.idToken
            val displayName = googleIdTokenCredential.displayName ?: ""

            val firebaseCredential = GoogleAuthProvider.getCredential(idTokenString, null)
            val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()

            val user = authResult.user
            if (user != null) {
                Log.d(TAG, "Google Sign-In successful. Firebase User ID: ${user.uid}, Email: ${user.email}")
                withContext(NonCancellable) {
                    val name = if (displayName.isNotEmpty()) displayName else (user.displayName ?: "")
                    ensureProfileExists(user.uid, user.email ?: "", name)
                }
            }
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
        try {
            firebaseAuth.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Sign out error: ${e.message}")
        }
        preferenceRepository.setGuestMode(false)
        preferenceRepository.setCachedSignedIn(false)
    }

    suspend fun sendPasswordResetEmail(email: String) {
        try {
            firebaseAuth.sendPasswordResetEmail(email).await()
        } catch (e: Exception) {
            Log.e(TAG, "Reset password error: ${e.message}")
        }
    }

    suspend fun confirmPasswordReset(oobCode: String, newPassword: String) {
    }

    suspend fun reauthenticate(password: String) {
    }

    suspend fun updatePassword(newPassword: String) {
        try {
            firebaseAuth.currentUser?.updatePassword(newPassword)?.await()
        } catch (e: Exception) {
            Log.e(TAG, "Update password error: ${e.message}")
        }
    }

    suspend fun deleteAccount() {
        val uid = getUserId() ?: return
        try {
            firestore.collection("users").document(uid).delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "Delete user document error: ${e.message}")
        }
        try {
            firebaseAuth.currentUser?.delete()?.await()
        } catch (e: Exception) {
            Log.e(TAG, "Delete auth user error: ${e.message}")
        }
        signOut()
    }

    fun isPasswordUser(): Boolean {
        return true
    }
}

sealed class AuthEvent {
    object SyncCompleted : AuthEvent()
}
