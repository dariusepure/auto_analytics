package com.dariusepure.caractivitylog.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ModernAppLogo(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    iconSize: Dp = 48.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Subtle inner shadow effect using a smaller box
        Box(
            modifier = Modifier
                .size(size * 0.9f)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.1f))
        )
        
        Icon(
            imageVector = Icons.Rounded.DirectionsCar,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}
