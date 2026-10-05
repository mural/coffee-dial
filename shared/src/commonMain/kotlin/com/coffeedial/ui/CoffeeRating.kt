package com.coffeedial.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private val CoffeeCup = ImageVector.Builder("Coffee", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(3f, 7f)
        lineTo(17f, 7f)
        lineTo(17f, 8f)
        lineTo(19f, 8f)
        curveTo(24f, 8f, 24f, 15f, 17f, 15f)
        curveTo(16f, 21f, 4f, 21f, 3f, 15f)
        close()
        moveTo(17f, 10f)
        lineTo(17f, 13f)
        curveTo(21f, 13f, 21f, 10f, 17f, 10f)
        close()
        moveTo(2f, 21f)
        lineTo(19f, 21f)
        lineTo(19f, 23f)
        lineTo(2f, 23f)
        close()
        moveTo(6f, 1f)
        lineTo(8f, 1f)
        lineTo(8f, 5f)
        lineTo(6f, 5f)
        close()
        moveTo(12f, 1f)
        lineTo(14f, 1f)
        lineTo(14f, 5f)
        lineTo(12f, 5f)
        close()
    }
}.build()

@Composable
internal fun CoffeeRating(rating: Int, onChange: ((Int) -> Unit)? = null, enabled: Boolean = true) {
    val modifier = if (onChange == null) {
        Modifier.clearAndSetSemantics {
            contentDescription = "Puntuación: $rating de 5"
        }
    } else {
        Modifier
    }
    Row(modifier) {
        (1..5).forEach { value ->
            val color = if (value <= rating) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
            }
            if (onChange == null) {
                Icon(CoffeeCup, null, Modifier.size(22.dp), tint = color)
            } else {
                IconButton(
                    onClick = { onChange(value) },
                    enabled = enabled,
                    modifier = Modifier.semantics {
                        contentDescription = "Puntuar con $value de 5 cafés"
                        selected = value == rating
                    }
                ) {
                    Icon(CoffeeCup, null, tint = color)
                }
            }
        }
    }
}
