package com.dariusepure.caractivitylog.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Custom Check Engine icon vector matching the engine silhouette design.
 */
val CheckEngineIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "CheckEngineIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            // Top Bar - Top edge & Right cap
            moveTo(8.0f, 2.0f)
            horizontalLineTo(16.0f)
            curveTo(16.88f, 2.0f, 17.6f, 2.72f, 17.6f, 3.5f)
            curveTo(17.6f, 4.28f, 16.88f, 5.0f, 16.0f, 5.0f)
            horizontalLineTo(14.5f)

            // Right top leg
            verticalLineTo(6.2f)

            // Top-right shoulder slope
            lineTo(17.5f, 9.0f)

            // Top-right junction notch
            horizontalLineTo(18.2f)
            verticalLineTo(11.0f)
            horizontalLineTo(19.2f)
            verticalLineTo(9.5f)

            // Right extension block - Top edge & Chamfer
            horizontalLineTo(21.2f)
            lineTo(22.8f, 11.1f)

            // Right extension block - Right edge & Bottom Chamfer
            verticalLineTo(16.4f)
            lineTo(21.2f, 18.0f)

            // Right extension block - Bottom edge & Bottom junction notch
            horizontalLineTo(19.2f)
            verticalLineTo(16.5f)
            horizontalLineTo(18.2f)
            verticalLineTo(18.5f)

            // Bottom Oil Pan - Right slope & Flat base
            lineTo(16.8f, 21.0f)
            horizontalLineTo(8.8f)

            // Bottom-Left slope
            lineTo(4.8f, 16.0f)

            // Bottom-left bridge
            verticalLineTo(14.7f)
            horizontalLineTo(2.8f)

            // Left Bar - Lower half & Bottom cap
            verticalLineTo(18.0f)
            curveTo(2.8f, 18.88f, 2.08f, 19.6f, 1.2f, 19.6f)
            curveTo(0.32f, 19.6f, 0.4f, 18.88f, 0.4f, 18.0f)

            // Left Bar - Left edge
            verticalLineTo(7.0f)

            // Left Bar - Top cap
            curveTo(0.4f, 6.12f, 1.12f, 5.4f, 2.0f, 5.4f)
            curveTo(2.88f, 5.4f, 2.8f, 6.12f, 2.8f, 7.0f)

            // Left Bar - Upper right edge & Top-left bridge
            verticalLineTo(9.2f)
            horizontalLineTo(4.8f)

            // Top-left shoulder slope
            lineTo(9.5f, 6.2f)

            // Left top leg
            verticalLineTo(5.0f)

            // Top Bar - Under left side & Left cap
            horizontalLineTo(8.0f)
            curveTo(7.12f, 5.0f, 6.4f, 4.28f, 6.4f, 3.5f)
            curveTo(6.4f, 2.72f, 7.12f, 2.0f, 8.0f, 2.0f)

            close()
        }
    }.build()
}
