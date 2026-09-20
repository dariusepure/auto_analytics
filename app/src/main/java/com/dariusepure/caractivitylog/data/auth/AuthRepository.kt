package com.dariusepure.caractivitylog.data.auth

import android.content.Context
import android.util.Log
import android.app.Activity
import android.content.ContextWrapper
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.dariusepure.caractivitylog.domain.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.gotrue.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val preferenceRepository: com.dariusepure.caractivitylog.data.prefs.PreferenceRepository
) {
    private val TAG = "AuthRepository"

    private val _authEvents = MutableSharedFlow<AuthEvent>(replay = 0)
    val authEvents: SharedFlow<AuthEvent> = _authEvents.asSharedFlow()

    companion object {
        const val GUEST_UID = "local_guest_user"
    }

    val signedIn: Flow<Boolean> = supabaseClient.auth.sessionStatus.map {
        it is SessionStatus.Authenticated
    }.distinctUntilChanged()

    val userId: Flow<String?> = signedIn.combine(preferenceRepository.isGuestMode) { signedInUser, isGuest ->
        if (isGuest) GUEST_UID
        else if (signedInUser) supabaseClient.auth.currentUserOrNull()?.id
        else null
    }.distinctUntilChanged()

    val userEmailFlow: Flow<String?> = signedIn.map { 
        if (it) supabaseClient.auth.currentUserOrNull()?.email else null 
    }.distinctUntilChanged()

    val isAnonymousFlow: Flow<Boolean> = signedIn.combine(preferenceRepository.isGuestMode) { _, isGuest ->
        isGuest
    }.distinctUntilChanged()

    val isCurrentlySignedIn: Boolean
        get() = supabaseClient.auth.currentUserOrNull() != null

    val currentUserEmail: String?
        get() = supabaseClient.auth.currentUserOrNull()?.email

    val isAnonymous: Boolean
        get() = false

    val isGuestMode: Flow<Boolean> = preferenceRepository.isGuestMode

    val isCurrentlyGuest: Boolean
        get() = preferenceRepository.isGuestMode.value

    fun getUserId(): String? {
        if (isCurrentlyGuest) return GUEST_UID
        return supabaseClient.auth.currentUserOrNull()?.id
    }

    fun getUserData(uid: String): Flow<User?> = flow {
        if (uid == GUEST_UID) {
            Log.d(TAG, "getUserData: Guest mode, emitting Guest user")
            emit(User(GUEST_UID, "", "Guest"))
            return@flow
        }
        
        try {
            Log.d(TAG, "getUserData: Fetching profile for UID: $uid")
            val userDto = supabaseClient.postgrest["profiles"]
                .select {
                    filter {
                        eq("id", uid)
                    }
                }
                .decodeSingleOrNull<FirestoreUser>()
            
            if (userDto == null) {
                val email = currentUserEmail ?: ""
                Log.w(TAG, "getUserData: Profile NOT FOUND for $uid. Email: $email. Emitting default 'User' profile.")
                emit(User(uid, email, "User"))
            } else {
                val user = userDto.fromFirebase()
                Log.d(TAG, "getUserData: Profile found: ${user.name} (${user.email})")
                emit(user)
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
        supabaseClient.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        val uid = supabaseClient.auth.currentUserOrNull()?.id ?: "unknown_uid"
        
        withContext(NonCancellable) {
            val user = FirestoreUser(
                id = uid,
                email = email,
                name = name
            )

            try {
                supabaseClient.postgrest["profiles"].insert(user)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to insert user to profiles: ${e.message}")
            }
            linkOldAccountIfNecessary(uid, email)
        }
    }

    suspend fun signIn(email: String, password: String) {
        supabaseClient.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        val user = supabaseClient.auth.currentUserOrNull()
        if (user != null) {
            withContext(NonCancellable) {
                linkOldAccountIfNecessary(user.id, user.email ?: "")
            }
        }
    }

    suspend fun signInWithGoogle(context: Context) {
        val webClientId = com.dariusepure.caractivitylog.BuildConfig.WEB_CLIENT_ID.trim()
        Log.d(TAG, "Starting Google Sign-In with WEB_CLIENT_ID: '$webClientId'")

        val activity = findActivity(context) ?: throw IllegalStateException("Context is not an Activity")
        val credentialManager = CredentialManager.create(activity)

        try {
            Log.d(TAG, "Clearing cached credential state...")
            credentialManager.clearCredentialState(androidx.credentials.ClearCredentialStateRequest())
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
            val idTokenString = googleIdTokenCredential.idToken
            val displayName = googleIdTokenCredential.displayName ?: ""

            supabaseClient.auth.signInWith(io.github.jan.supabase.gotrue.providers.builtin.IDToken) {
                idToken = idTokenString
                provider = io.github.jan.supabase.gotrue.providers.Google
            }

            val user = supabaseClient.auth.currentUserOrNull()
            if (user != null) {
                Log.d(TAG, "Google Sign-In successful. Supabase User ID: ${user.id}, Email: ${user.email}")
                withContext(NonCancellable) {
                    val firestoreUser = FirestoreUser(
                        id = user.id,
                        email = user.email ?: "",
                        name = if (displayName.isNotEmpty()) displayName else (user.userMetadata?.get("full_name")?.toString() ?: "")
                    )
                    try {
                        Log.d(TAG, "processCredentialResult: Inserting profile for ${user.id}...")
                        supabaseClient.postgrest["profiles"].upsert(firestoreUser)
                        Log.d(TAG, "processCredentialResult: Profile sync successful")
                    } catch (e: Exception) {
                        Log.w(TAG, "User profile sync error: ${e.message}")
                    }
                    linkOldAccountIfNecessary(user.id, user.email ?: "")
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
            runBlocking {
                supabaseClient.auth.signOut()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sign out error: ${e.message}")
        }
        preferenceRepository.setGuestMode(false)
    }

    suspend fun sendPasswordResetEmail(email: String) {
        try {
            supabaseClient.auth.resetPasswordForEmail(email)
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
            supabaseClient.auth.updateUser {
                this.password = newPassword
            }
        } catch (e: Exception) {
            Log.e(TAG, "Update password error: ${e.message}")
        }
    }

    suspend fun deleteAccount() {
        val uid = getUserId() ?: return
        try {
            supabaseClient.postgrest["profiles"].delete {
                filter {
                    eq("id", uid)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Delete profiles table error: ${e.message}")
        }
        signOut()
    }

    private suspend fun linkOldAccountIfNecessary(newUid: String, email: String) {
        if (email.isBlank()) return
        Log.d(TAG, "Checking for old accounts to link for email: $email...")
        try {
            // Căutăm TOATE profilurile care au acest email (inclusiv cel curent)
            val allProfilesWithEmail = supabaseClient.postgrest["profiles"]
                .select {
                    filter {
                        eq("email", email)
                    }
                }
                .decodeList<FirestoreUser>()
            
            Log.d(TAG, "Found ${allProfilesWithEmail.size} total profiles for $email")

            for (oldProfile in allProfilesWithEmail) {
                if (oldProfile.id == newUid) continue // Sărim peste cel curent
                
                val oldUid = oldProfile.id
                Log.d(TAG, "Linking old account ID: $oldUid to new ID: $newUid")

                // 2. Actualizăm mașinile din tabela 'cars'
                // Încercăm ambele variante de coloană pentru siguranță (id sau user_id)
                try {
                    val updateResult1 = supabaseClient.postgrest["cars"].update(
                        mapOf("user_id" to newUid)
                    ) {
                        filter {
                            eq("user_id", oldUid)
                        }
                    }
                    Log.d(TAG, "Updated cars (user_id column) from $oldUid to $newUid")
                    
                    val updateResult2 = supabaseClient.postgrest["cars"].update(
                        mapOf("id" to newUid)
                    ) {
                        filter {
                            eq("id", oldUid)
                        }
                    }
                    Log.d(TAG, "Updated cars (id column) from $oldUid to $newUid")
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating car ownership: ${e.message}")
                }

                // 3. Ștergem profilul vechi deoarece datele au fost migrate
                try {
                    supabaseClient.postgrest["profiles"].delete {
                        filter {
                            eq("id", oldUid)
                        }
                    }
                    Log.d(TAG, "Deleted old profile $oldUid")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete old profile $oldUid")
                }
            }
            
            // Emitem evenimentul pentru a forța reîncărcarea mașinilor în CarRepository
            _authEvents.emit(AuthEvent.SyncCompleted)
            
        } catch (e: Exception) {
            Log.e(TAG, "Account linking error: ${e.message}", e)
        }
    }

    fun isPasswordUser(): Boolean {
        return true
    }
}

sealed class AuthEvent {
    object SyncCompleted : AuthEvent()
}
