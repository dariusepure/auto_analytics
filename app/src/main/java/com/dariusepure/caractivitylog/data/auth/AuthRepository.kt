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
        val isAuthenticated = it is SessionStatus.Authenticated
        preferenceRepository.setCachedSignedIn(isAuthenticated)
        isAuthenticated
    }.distinctUntilChanged()

    val initialSignedIn: Boolean
        get() = preferenceRepository.cachedSignedIn.value || preferenceRepository.isGuestMode.value

    val userId: Flow<String?> = signedIn.combine(preferenceRepository.isGuestMode) { signedInUser, isGuest ->
        val currentUser = supabaseClient.auth.currentUserOrNull()
        if (signedInUser && currentUser != null) {
            if (isGuest) preferenceRepository.setGuestMode(false)
            currentUser.id
        } else if (isGuest) GUEST_UID
        else null
    }.distinctUntilChanged()

    val userEmailFlow: Flow<String?> = signedIn.map { 
        if (it) supabaseClient.auth.currentUserOrNull()?.email else null 
    }.distinctUntilChanged()

    val isAnonymousFlow: Flow<Boolean> = signedIn.combine(preferenceRepository.isGuestMode) { signedInUser, isGuest ->
        if (signedInUser) false else isGuest
    }.distinctUntilChanged()

    val isCurrentlySignedIn: Boolean
        get() = supabaseClient.auth.currentUserOrNull() != null

    val currentUserEmail: String?
        get() = supabaseClient.auth.currentUserOrNull()?.email

    val isAnonymous: Boolean
        get() = false

    val isGuestMode: Flow<Boolean> = preferenceRepository.isGuestMode

    val isCurrentlyGuest: Boolean
        get() = !isCurrentlySignedIn && preferenceRepository.isGuestMode.value

    fun getUserId(): String? {
        val currentUser = supabaseClient.auth.currentUserOrNull()
        if (currentUser != null) return currentUser.id
        if (isCurrentlyGuest) return GUEST_UID
        return null
    }

    suspend fun ensureProfileExists(uid: String, email: String, name: String = "") {
        if (uid.isBlank() || uid == "unknown_uid" || uid == GUEST_UID || uid == "guest") return
        try {
            val displayName = name.ifBlank {
                supabaseClient.auth.currentUserOrNull()?.userMetadata?.get("full_name")?.toString()
                    ?: email.substringBefore("@")
            }
            val user = RemoteUser(
                id = uid,
                email = email.ifBlank { currentUserEmail ?: "" },
                name = displayName.ifBlank { "User" }
            )
            supabaseClient.postgrest["profiles"].upsert(user)
            Log.d(TAG, "ensureProfileExists: Upserted profile for $uid (${user.email})")
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
            val userDto = supabaseClient.postgrest["profiles"]
                .select {
                    filter {
                        eq("id", uid)
                    }
                }
                .decodeSingleOrNull<RemoteUser>()
            
            if (userDto == null) {
                val email = currentUserEmail ?: ""
                val name = email.substringBefore("@").ifBlank { "User" }
                Log.w(TAG, "getUserData: Profile NOT FOUND for $uid. Email: $email. Creating profile now.")
                emit(User(uid, email, name))
                ensureProfileExists(uid, email, name)
            } else {
                val user = userDto.fromRemote()
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
        preferenceRepository.setGuestMode(false)
        val userInfo = supabaseClient.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        val currentUser = supabaseClient.auth.currentUserOrNull()
        val uid = currentUser?.id ?: userInfo?.id ?: "unknown_uid"
        
        withContext(NonCancellable) {
            if (uid != "unknown_uid") {
                ensureProfileExists(uid, email, name)
                linkOldAccountIfNecessary(uid, email)
            }
        }
    }

    suspend fun signIn(email: String, password: String) {
        preferenceRepository.setGuestMode(false)
        supabaseClient.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        val user = supabaseClient.auth.currentUserOrNull()
        if (user != null) {
            withContext(NonCancellable) {
                ensureProfileExists(user.id, user.email ?: "")
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
        preferenceRepository.setGuestMode(false)
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
                    val name = if (displayName.isNotEmpty()) displayName else (user.userMetadata?.get("full_name")?.toString() ?: "")
                    ensureProfileExists(user.id, user.email ?: "", name)
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
        if (email.isBlank() || newUid == "unknown_uid" || newUid == GUEST_UID || newUid == "guest") return
        Log.d(TAG, "Checking for old accounts to link for email: $email...")
        try {
            ensureProfileExists(newUid, email)

            // Căutăm TOATE profilurile care au acest email (inclusiv cel curent)
            val allProfilesWithEmail = supabaseClient.postgrest["profiles"]
                .select {
                    filter {
                        eq("email", email)
                    }
                }
                .decodeList<RemoteUser>()
            
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
