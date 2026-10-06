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

internal val CoffeeCup = ImageVector.Builder("Coffee", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(2f, 19f)
        lineTo(20f, 19f)
        lineTo(20f, 21f)
        lineTo(2f, 21f)
        close()
        moveTo(4f, 3f)
        lineTo(16f, 3f)
        lineTo(16f, 13f)
        curveTo(16f, 15.2f, 14.2f, 17f, 12f, 17f)
        lineTo(8f, 17f)
        curveTo(5.8f, 17f, 4f, 15.2f, 4f, 13f)
        close()
        moveTo(18f, 5f)
        lineTo(20f, 5f)
        curveTo(21.1f, 5f, 22f, 5.9f, 22f, 7f)
        lineTo(22f, 11f)
        curveTo(22f, 12.1f, 21.1f, 13f, 20f, 13f)
        lineTo(18f, 13f)
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
