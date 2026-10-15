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
    val strings = LocalStrings.current
    val selected = grindLevels.indexOfFirst { canonical ->
        val trimmed = grind.trim()
        canonical.equals(trimmed, ignoreCase = true) ||
            strings.grindLevelLabel(canonical).equals(trimmed, ignoreCase = true)
    }
    val parsed = ProGrindSetting.parse(grind)
    var expanded by rememberSaveable { mutableStateOf(parsed != null) }
    val enabled = !saving
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(strings.grind, style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Fino", "Medio", "Grueso").forEach { level ->
                val label = strings.grindLevelLabel(level)
                FilterChip(
                    selected = level.equals(grind.trim(), ignoreCase = true) ||
                        label.equals(grind.trim(), ignoreCase = true),
                    onClick = { onChange(level) },
                    enabled = enabled,
                    label = { Text(label) }
                )
            }
        }
        // A custom numeric adjustment has no universal equivalent on this scale.
        if (selected >= 0) {
            val levelLabel = strings.grindLevelLabel(grindLevels[selected])
            Text(levelLabel, color = MaterialTheme.colorScheme.primary)
            Slider(
                value = selected.toFloat(),
                onValueChange = {
                    onChange(grindLevels[it.roundToInt().coerceIn(grindLevels.indices)])
                },
                valueRange = 0f..grindLevels.lastIndex.toFloat(),
                steps = grindLevels.size - 2,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().semantics {
                    contentDescription = strings.grindKind
                    stateDescription = levelLabel
                }
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(strings.veryFine, style = MaterialTheme.typography.labelSmall)
                Text(strings.veryCoarse, style = MaterialTheme.typography.labelSmall)
            }
        }
        Field(strings.millAdjustment, grind, error, saving) { onChange(it) }
        Text(strings.grindHint, style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = { expanded = !expanded }, enabled = enabled) {
            Text(if (expanded) strings.hideProGrind else strings.showProGrind)
        }
        if (expanded) {
            val pro = parsed ?: ProGrindSetting(0, 0, 0)
            Text(strings.proGrindTitle, style = MaterialTheme.typography.titleSmall)
            Text(strings.proGrindHint, style = MaterialTheme.typography.bodySmall)
            GrindTuner(strings.rotations, pro.rotations, enabled) {
                onChange(pro.copy(rotations = it).notation())
            }
            GrindTuner(strings.dial, pro.dial, enabled) {
                onChange(pro.copy(dial = it).notation())
            }
            GrindTuner(strings.clicks, pro.clicks, enabled) {
                onChange(pro.copy(clicks = it).notation())
            }
            Text(
                if (parsed == null) {
                    strings.tapToApplyPro
                } else {
                    strings.savedAdjustment(grind)
                },
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun GrindTuner(label: String, value: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    val strings = LocalStrings.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextButton(
            onClick = { onChange(value - 1) },
            enabled = enabled && value > 0,
            modifier = Modifier.semantics { contentDescription = strings.decreaseLabel(label) }
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
            modifier = Modifier.semantics { contentDescription = strings.increaseLabel(label) }
        ) { Text("+") }
    }
}
