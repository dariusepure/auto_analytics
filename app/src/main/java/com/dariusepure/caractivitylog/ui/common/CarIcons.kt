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

/**
 * Custom Drivetrain Chassis icon vector representing 4 wheels with axles and drive shaft.
 */
val DrivetrainChassisIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "DrivetrainChassisIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // Four Wheels
        path(fill = SolidColor(Color.Black)) {
            // Top-Left Wheel
            moveTo(4.0f, 2.5f)
            horizontalLineTo(5.0f)
            curveTo(5.55f, 2.5f, 6.0f, 2.95f, 6.0f, 3.5f)
            verticalLineTo(7.5f)
            curveTo(6.0f, 8.05f, 5.55f, 8.5f, 5.0f, 8.5f)
            horizontalLineTo(4.0f)
            curveTo(3.45f, 8.5f, 3.0f, 8.05f, 3.0f, 7.5f)
            verticalLineTo(3.5f)
            curveTo(3.0f, 2.95f, 3.45f, 2.5f, 4.0f, 2.5f)
            close()

            // Top-Right Wheel
            moveTo(19.0f, 2.5f)
            horizontalLineTo(20.0f)
            curveTo(20.55f, 2.5f, 21.0f, 2.95f, 21.0f, 3.5f)
            verticalLineTo(7.5f)
            curveTo(21.0f, 8.05f, 20.55f, 8.5f, 20.0f, 8.5f)
            horizontalLineTo(19.0f)
            curveTo(18.45f, 8.5f, 18.0f, 8.05f, 18.0f, 7.5f)
            verticalLineTo(3.5f)
            curveTo(18.0f, 2.95f, 18.45f, 2.5f, 19.0f, 2.5f)
            close()

            // Bottom-Left Wheel
            moveTo(4.0f, 15.5f)
            horizontalLineTo(5.0f)
            curveTo(5.55f, 15.5f, 6.0f, 15.95f, 6.0f, 16.5f)
            verticalLineTo(20.5f)
            curveTo(6.0f, 21.05f, 5.55f, 21.5f, 5.0f, 21.5f)
            horizontalLineTo(4.0f)
            curveTo(3.45f, 21.5f, 3.0f, 21.05f, 3.0f, 20.5f)
            verticalLineTo(16.5f)
            curveTo(3.0f, 15.95f, 3.45f, 15.5f, 4.0f, 15.5f)
            close()

            // Bottom-Right Wheel
            moveTo(19.0f, 15.5f)
            horizontalLineTo(20.0f)
            curveTo(20.55f, 15.5f, 21.0f, 15.95f, 21.0f, 16.5f)
            verticalLineTo(20.5f)
            curveTo(21.0f, 21.05f, 20.55f, 21.5f, 20.0f, 21.5f)
            horizontalLineTo(19.0f)
            curveTo(18.45f, 21.5f, 18.0f, 21.05f, 18.0f, 20.5f)
            verticalLineTo(16.5f)
            curveTo(18.0f, 15.95f, 18.45f, 15.5f, 19.0f, 15.5f)
            close()
        }

        // Chassis Frame, Axles, Drive Shaft & Differentials
        path(fill = SolidColor(Color.Black)) {
            // Front Axle
            moveTo(6.0f, 4.8f)
            horizontalLineTo(18.0f)
            verticalLineTo(6.2f)
            horizontalLineTo(6.0f)
            close()

            // Rear Axle
            moveTo(6.0f, 17.8f)
            horizontalLineTo(18.0f)
            verticalLineTo(19.2f)
            horizontalLineTo(6.0f)
            close()

            // Central Drive Shaft
            moveTo(11.3f, 5.5f)
            horizontalLineTo(12.7f)
            verticalLineTo(18.5f)
            horizontalLineTo(11.3f)
            close()

            // Transfer Case / Central Differential
            moveTo(10.0f, 10.0f)
            horizontalLineTo(14.0f)
            curveTo(14.55f, 10.0f, 15.0f, 10.45f, 15.0f, 11.0f)
            verticalLineTo(13.0f)
            curveTo(15.0f, 13.55f, 14.55f, 14.0f, 14.0f, 14.0f)
            horizontalLineTo(10.0f)
            curveTo(9.45f, 14.0f, 9.0f, 13.55f, 9.0f, 13.0f)
            verticalLineTo(11.0f)
            curveTo(9.0f, 10.45f, 9.45f, 10.0f, 10.0f, 10.0f)
            close()

            // Front Differential Box
            moveTo(10.5f, 4.3f)
            horizontalLineTo(13.5f)
            curveTo(13.8f, 4.3f, 14.0f, 4.5f, 14.0f, 4.8f)
            verticalLineTo(6.2f)
            curveTo(14.0f, 6.5f, 13.8f, 6.7f, 13.5f, 6.7f)
            horizontalLineTo(10.5f)
            curveTo(10.2f, 6.7f, 10.0f, 6.5f, 10.0f, 6.2f)
            verticalLineTo(4.8f)
            curveTo(10.0f, 4.5f, 10.2f, 4.3f, 10.5f, 4.3f)
            close()

            // Rear Differential Box
            moveTo(10.5f, 17.3f)
            horizontalLineTo(13.5f)
            curveTo(13.8f, 17.3f, 14.0f, 17.5f, 14.0f, 17.8f)
            verticalLineTo(19.2f)
            curveTo(14.0f, 19.5f, 13.8f, 19.7f, 13.5f, 19.7f)
            horizontalLineTo(10.5f)
            curveTo(10.2f, 19.7f, 10.0f, 19.5f, 10.0f, 19.2f)
            verticalLineTo(17.8f)
            curveTo(10.0f, 17.5f, 10.2f, 17.3f, 10.5f, 17.3f)
            close()
        }
    }.build()
}
