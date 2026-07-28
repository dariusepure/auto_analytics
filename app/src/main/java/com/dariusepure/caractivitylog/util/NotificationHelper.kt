package com.dariusepure.caractivitylog.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dariusepure.caractivitylog.R

object NotificationHelper {
    private const val CHANNEL_ID = "itp_notifications"
    private const val CHANNEL_NAME = "ITP Expiration Alerts"
    private const val CHANNEL_DESC = "Notifications for vehicle inspection expiration"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showItpNotification(context: Context, carName: String, daysLeft: Int) {
        val title = "ITP Expiration Alert"
        val message = when {
            daysLeft == 1 -> "Your ITP for $carName expires tomorrow!"
            daysLeft == 0 -> "Your ITP for $carName expires today!"
            daysLeft < 0 -> "Your ITP for $carName has expired!"
            else -> "Your ITP for $carName expires in $daysLeft days."
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(carName.hashCode(), builder.build())
            }
        } catch (e: SecurityException) {
            // Permission not granted, can't show notification
        }
    }
}
