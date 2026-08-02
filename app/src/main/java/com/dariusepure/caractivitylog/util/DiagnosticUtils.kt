package com.dariusepure.caractivitylog.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import android.util.Log
import java.security.MessageDigest

object DiagnosticUtils {
    fun getAppSignatureSha1(context: Context, withColons: Boolean = true): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            val signature = signatures?.firstOrNull() ?: return "No signature found"
            val md = MessageDigest.getInstance("SHA-1")
            val digest = md.digest(signature.toByteArray())
            
            if (withColons) {
                digest.joinToString(":") { "%02X".format(it) }
            } else {
                digest.joinToString("") { "%02X".format(it) }.lowercase()
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
