package com.coffeedial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import com.coffeedial.ui.CoffeeTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.coffeedial.domain.ProGrindSetting
import com.coffeedial.domain.grindLevels
import kotlin.math.roundToInt

@Composable
fun GrindSettingField(grind: String, error: String?, saving: Boolean, onChange: (String) -> Unit) {
    val selected = grindLevels.indexOfFirst { it.equals(grind.trim(), ignoreCase = true) }
    val parsed = ProGrindSetting.parse(grind)
    var expanded by rememberSaveable { mutableStateOf(parsed != null) }
    val enabled = !saving
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Molienda", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Fino", "Medio", "Grueso").forEach { level ->
                FilterChip(
                    selected = level.equals(grind.trim(), ignoreCase = true),
                    onClick = { onChange(level) },
                    enabled = enabled,
                    label = { Text(level) }
                )
            }
        }
        // A custom numeric adjustment has no universal equivalent on this scale.
        if (selected >= 0) {
            Text(grindLevels[selected], color = MaterialTheme.colorScheme.primary)
            Slider(
                value = selected.toFloat(),
                onValueChange = {
                    onChange(grindLevels[it.roundToInt().coerceIn(grindLevels.indices)])
                },
                valueRange = 0f..grindLevels.lastIndex.toFloat(),
                steps = grindLevels.size - 2,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().semantics {
                    contentDescription = "Tipo de molienda"
                    stateDescription = grindLevels[selected]
                }
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Muy fino", style = MaterialTheme.typography.labelSmall)
                Text("Muy grueso", style = MaterialTheme.typography.labelSmall)
            }
        }
        Field("Ajuste del molino", grind, error, saving) { onChange(it) }
        Text(
            "Elegí un nivel o anotá el ajuste real: 18 clics, 2.3, 1.4.2…",
            style = MaterialTheme.typography.bodySmall
        )
        TextButton(onClick = { expanded = !expanded }, enabled = enabled) {
            Text(if (expanded) "Ocultar ajustes pro" else "Mostrar ajustes pro")
        }
        if (expanded) {
            val pro = parsed ?: ProGrindSetting(0, 0, 0)
            Text("Vueltas · Dial · Clics", style = MaterialTheme.typography.titleSmall)
            Text(
                "Usá esta notación si tu molino la admite. Cada escala es propia del equipo; no convierte fino o grueso a clics.",
                style = MaterialTheme.typography.bodySmall
            )
            GrindTuner("Vueltas", pro.rotations, enabled) {
                onChange(pro.copy(rotations = it).notation())
            }
            GrindTuner("Dial", pro.dial, enabled) { onChange(pro.copy(dial = it).notation()) }
            GrindTuner("Clics", pro.clicks, enabled) { onChange(pro.copy(clicks = it).notation()) }
            Text(
                if (parsed ==
                    null
                ) {
                    "Tocá un control para aplicar el ajuste pro; tu valor actual todavía se conserva."
                } else {
                    "Ajuste guardado: $grind"
                },
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun GrindTuner(label: String, value: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextButton(
            onClick = { onChange(value - 1) },
            enabled = enabled && value > 0,
            modifier = Modifier.semantics { contentDescription = "Reducir $label" }
        ) { Text("-") }
        OutlinedTextField(
            value = value.toString(),
            onValueChange = { text ->
                if (text.isEmpty()) {
                    onChange(0)
                } else if (text.length <= 4 && text.all { it in '0'..'9' }) {
                    onChange(text.toInt())
                }
            },
            label = { Text(label) },
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
        TextButton(
            onClick = { onChange(value + 1) },
            enabled = enabled && value < 9999,
            modifier = Modifier.semantics { contentDescription = "Aumentar $label" }
        ) { Text("+") }
    }
}
