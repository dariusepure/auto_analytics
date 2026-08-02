package com.dariusepure.caractivitylog.ui.common

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

fun Modifier.shimmer(): Modifier = composed {
    // Disable complex animations on very old/weak devices to save CPU
    val isLowEnd = Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q 
    
    if (isLowEnd) {
        val transition = rememberInfiniteTransition(label = "pulse")
        val alpha by transition.animateFloat(
            initialValue = 0.2f,
            targetValue = 0.4f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alphaPulse"
        )
        background(Color.LightGray.copy(alpha = alpha))
    } else {
        val transition = rememberInfiniteTransition(label = "shimmer")
        val translateAnim = transition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmerTranslate"
        )

        val shimmerColors = listOf(
            Color.LightGray.copy(alpha = 0.6f),
            Color.LightGray.copy(alpha = 0.2f),
            Color.LightGray.copy(alpha = 0.6f),
        )

        val brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset.Zero,
            end = Offset(x = translateAnim.value, y = translateAnim.value)
        )

        background(brush)
    }
}

@Composable
fun CarCardSkeleton() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(Color.LightGray.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .shimmer()
    )
}

@Composable
fun CarDetailsSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Box(modifier = Modifier.size(64.dp).background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(32.dp)).shimmer())
            Spacer(Modifier.width(16.dp))
            Box(modifier = Modifier.width(150.dp).height(32.dp).background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(4.dp)).shimmer())
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Bento Grid Mockup
        Row(modifier = Modifier.fillMaxWidth().height(160.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1.5f).fillMaxHeight().background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(16.dp)).shimmer())
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(16.dp)).shimmer())
        }
        
        Row(modifier = Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(16.dp)).shimmer())
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(16.dp)).shimmer())
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(16.dp)).shimmer())
        }
    }
}
