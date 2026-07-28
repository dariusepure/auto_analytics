package com.dariusepure.caractivitylog.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Empty state with an icon, title, subtitle, and optional action button.
 */
@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Inbox,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp).height(72.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (action != null) {
            Spacer(Modifier.height(20.dp))
            action()
        }
    }
}

/**
 * "just now" / "5m ago" / "3h ago" / "2d ago" / "Jul 12".
 */
fun Date.toRelativeString(context: android.content.Context): String {
    val nowMs = System.currentTimeMillis()
    val thenMs = this.time
    val diffMs = (nowMs - thenMs).coerceAtLeast(0L)
    return when {
        diffMs < 60_000L -> context.getString(com.dariusepure.caractivitylog.R.string.relative_just_now)
        diffMs < 3_600_000L -> context.getString(com.dariusepure.caractivitylog.R.string.relative_minutes_ago, (diffMs / 60_000L).toInt())
        diffMs < 86_400_000L -> context.getString(com.dariusepure.caractivitylog.R.string.relative_hours_ago, (diffMs / 3_600_000L).toInt())
        diffMs < 7 * 86_400_000L -> context.getString(com.dariusepure.caractivitylog.R.string.relative_days_ago, (diffMs / 86_400_000L).toInt())
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(this)
    }
}