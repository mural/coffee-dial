package com.coffeedial.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private fun metricIcon(name: String, draw: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = draw
        )
    }.build()

internal val BeanIcon = metricIcon("Coffee bean") {
    moveTo(19f, 4f)
    curveTo(15f, 0f, 8f, 3f, 4.5f, 8f)
    curveTo(1f, 13f, 2f, 18f, 5f, 20f)
    curveTo(9f, 23f, 16f, 20f, 19.5f, 15f)
    curveTo(23f, 10f, 22f, 6f, 19f, 4f)
    close()
    moveTo(18f, 4f)
    curveTo(9f, 6f, 16f, 16f, 6f, 20f)
}

internal val MilkIcon = metricIcon("Milk carton") {
    moveTo(8f, 2f)
    lineTo(16f, 2f)
    lineTo(16f, 5f)
    lineTo(19f, 9f)
    lineTo(19f, 22f)
    lineTo(5f, 22f)
    lineTo(5f, 9f)
    lineTo(8f, 5f)
    close()
    moveTo(8f, 5f)
    lineTo(16f, 5f)
    moveTo(5f, 9f)
    lineTo(19f, 9f)
    moveTo(9f, 13f)
    lineTo(9f, 18f)
    lineTo(15f, 18f)
    lineTo(15f, 13f)
    close()
}

internal val WaterIcon = metricIcon("Water drop") {
    moveTo(12f, 2f)
    curveTo(10f, 6f, 5f, 11f, 5f, 15f)
    curveTo(5f, 19f, 8f, 22f, 12f, 22f)
    curveTo(16f, 22f, 19f, 19f, 19f, 15f)
    curveTo(19f, 11f, 14f, 6f, 12f, 2f)
    close()
    moveTo(8f, 15f)
    curveTo(8f, 17f, 9f, 18f, 11f, 19f)
}

internal val MugIcon = metricIcon("Total cup") {
    moveTo(4f, 5f)
    lineTo(17f, 5f)
    lineTo(17f, 16f)
    curveTo(17f, 19f, 15f, 21f, 12f, 21f)
    lineTo(9f, 21f)
    curveTo(6f, 21f, 4f, 19f, 4f, 16f)
    close()
    moveTo(17f, 7f)
    lineTo(19f, 7f)
    curveTo(23f, 7f, 23f, 14f, 19f, 14f)
    lineTo(17f, 14f)
}

internal val TimerIcon = metricIcon("Extraction time") {
    moveTo(20f, 13f)
    curveTo(20f, 17.4f, 16.4f, 21f, 12f, 21f)
    curveTo(7.6f, 21f, 4f, 17.4f, 4f, 13f)
    curveTo(4f, 8.6f, 7.6f, 5f, 12f, 5f)
    curveTo(16.4f, 5f, 20f, 8.6f, 20f, 13f)
    close()
    moveTo(12f, 9f)
    lineTo(12f, 13f)
    lineTo(15f, 15f)
    moveTo(9f, 2f)
    lineTo(15f, 2f)
    moveTo(12f, 2f)
    lineTo(12f, 5f)
}
