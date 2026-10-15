package com.coffeedial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.coffeedial.domain.MilkFoam
import com.coffeedial.ui.CoffeeTextButton as TextButton

@Composable
internal fun MilkFoamChoices(value: MilkFoam?, enabled: Boolean, onChange: (MilkFoam?) -> Unit) {
    val strings = LocalStrings.current
    Column {
        Text(strings.milkFoamOptional, style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MilkFoam.entries.forEach { option ->
                FilterChip(
                    selected = value == option,
                    onClick = { onChange(option) },
                    enabled = enabled,
                    label = { Text(option.label(strings)) }
                )
            }
            if (value != null) {
                TextButton(onClick = { onChange(null) }, enabled = enabled) { Text(strings.actionRemove) }
            }
        }
        Text(
            if (value == null) {
                strings.notRecorded
            } else {
                strings.milkFoamNote
            },
            style = MaterialTheme.typography.bodySmall
        )
    }
}
