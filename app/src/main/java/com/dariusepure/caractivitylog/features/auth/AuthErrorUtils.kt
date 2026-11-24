package com.dariusepure.caractivitylog.features.auth

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.dariusepure.caractivitylog.R

object AuthErrorUtils {

    fun getLocalizedAuthErrorMessage(context: Context, e: Throwable, defaultResId: Int): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        val contextToUse = if (!locales.isEmpty) {
            val config = android.content.res.Configuration(context.resources.configuration)
            config.setLocale(locales.get(0))
            context.createConfigurationContext(config)
        } else {
            context
        }

        return when (e) {
            is com.google.firebase.auth.FirebaseAuthWeakPasswordException -> contextToUse.getString(R.string.error_weak_password)
            is com.google.firebase.auth.FirebaseAuthInvalidUserException -> contextToUse.getString(R.string.error_user_not_found)
            is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> contextToUse.getString(R.string.error_invalid_credentials)
            is com.google.firebase.auth.FirebaseAuthUserCollisionException -> contextToUse.getString(R.string.error_email_already_in_use)
            is com.google.firebase.FirebaseNetworkException -> contextToUse.getString(R.string.error_network)
            is com.google.firebase.FirebaseTooManyRequestsException -> contextToUse.getString(R.string.error_too_many_requests)
            else -> {
                val msg = e.localizedMessage
                if (msg.isNullOrBlank()) {
                    contextToUse.getString(defaultResId)
                } else {
                    when {
                        msg.contains("user-not-found", ignoreCase = true) || msg.contains("no user record", ignoreCase = true) -> contextToUse.getString(R.string.error_user_not_found)
                        msg.contains("invalid-credential", ignoreCase = true) || msg.contains("invalid password", ignoreCase = true) || msg.contains("badly formatted", ignoreCase = true) -> contextToUse.getString(R.string.error_invalid_credentials)
                        msg.contains("email-already-in-use", ignoreCase = true) || msg.contains("already in use", ignoreCase = true) -> contextToUse.getString(R.string.error_email_already_in_use)
                        msg.contains("weak-password", ignoreCase = true) -> contextToUse.getString(R.string.error_weak_password)
                        msg.contains("network", ignoreCase = true) -> contextToUse.getString(R.string.error_network)
                        msg.contains("too-many-requests", ignoreCase = true) || msg.contains("blocked all requests", ignoreCase = true) -> contextToUse.getString(R.string.error_too_many_requests)
                        else -> msg
                    }
                }
            }
        }
    }
}
