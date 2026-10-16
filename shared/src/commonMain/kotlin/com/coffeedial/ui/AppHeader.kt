package com.coffeedial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import com.coffeedial.ui.CoffeeTextButton as TextButton

@Composable
internal fun AppHeader(
    screen: String,
    roots: Set<String>,
    saving: Boolean,
    busy: Boolean,
    headerOffset: Float,
    onHeightMeasured: (Int) -> Unit,
    onGoBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    Row(
        modifier.fillMaxWidth().clipToBounds().layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            onHeightMeasured(placeable.height)
            val offset = if (screen in roots) headerOffset.toInt() else 0
            layout(
                placeable.width,
                (placeable.height + offset).coerceAtLeast(0)
            ) {
                placeable.placeRelative(0, offset)
            }
        },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            strings.appName,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (screen !in roots) {
                TextButton(enabled = !saving && !busy, onClick = onGoBack) {
                    Text(strings.actionBack)
                }
            }
        }
    }
}
