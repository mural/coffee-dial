package com.coffeedial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.coffeedial.domain.MilkFoam

@Composable
internal fun MilkFoamChoices(value: MilkFoam?, enabled: Boolean, onChange: (MilkFoam?) -> Unit) {
    Column {
        Text("Espuma de leche (opcional)", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MilkFoam.entries.forEach { option ->
                FilterChip(
                    selected = value == option,
                    onClick = { onChange(option) },
                    enabled = enabled,
                    label = { Text(option.label) }
                )
            }
            if (value !=
                null
            ) {
                TextButton(onClick = { onChange(null) }, enabled = enabled) { Text("Quitar") }
            }
        }
        Text(
            if (value ==
                null
            ) {
                "Sin registrar"
            } else {
                "Describe la textura; no suma volumen al total."
            },
            style = MaterialTheme.typography.bodySmall
        )
    }
}
