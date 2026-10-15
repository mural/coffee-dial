package com.coffeedial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.coffeedial.domain.TasteImpression
import com.coffeedial.domain.TasteProfile
import com.coffeedial.ui.CoffeeTextButton as TextButton
import kotlin.math.roundToInt

internal fun TasteImpression.label(strings: AppStrings): String = when (this) {
    TasteImpression.TOO_SOUR -> strings.impressionTooSour
    TasteImpression.TOO_BITTER -> strings.impressionTooBitter
    TasteImpression.WEAK -> strings.impressionWeak
    TasteImpression.BALANCED -> strings.impressionBalanced
    TasteImpression.TOO_STRONG -> strings.impressionTooStrong
}

@Composable
fun TasteProfileEditor(value: TasteProfile?, enabled: Boolean, onChange: (TasteProfile?) -> Unit) {
    val strings = LocalStrings.current
    val profile = value ?: TasteProfile()
    fun update(next: TasteProfile) {
        onChange(next.takeUnless { it.isEmpty() })
    }
    CoffeeSection {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(strings.flavorProfile, style = MaterialTheme.typography.titleMedium)
            Text(strings.tasteOptionalNote, style = MaterialTheme.typography.bodyMedium)
            Column(Modifier.selectableGroup()) {
                TasteImpression.entries.forEach { impression ->
                    Row(
                        Modifier.fillMaxWidth().selectable(
                            selected = profile.impression == impression,
                            enabled = enabled,
                            role = Role.RadioButton,
                            onClick = { update(profile.copy(impression = impression)) }
                        ).padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            profile.impression == impression,
                            onClick = null,
                            enabled = enabled
                        )
                        Text(impression.label(strings))
                    }
                }
            }
            if (profile.impression != null) {
                TextButton(enabled = enabled, onClick = {
                    update(profile.copy(impression = null))
                }) { Text(strings.removeTasteRating) }
            }
            TasteSlider(strings.body, strings.bodyLight, strings.bodyFull, profile.body, enabled) {
                update(profile.copy(body = it))
            }
            TasteSlider(strings.acidity, strings.levelLow, strings.levelHigh, profile.acidity, enabled) {
                update(profile.copy(acidity = it))
            }
            TasteSlider(strings.bitterness, strings.levelLowMasculine, strings.levelHighMasculine, profile.bitterness, enabled) {
                update(profile.copy(bitterness = it))
            }
            Text(
                strings.tasteScaleNote,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun TasteSlider(
    title: String,
    low: String,
    high: String,
    value: Int?,
    enabled: Boolean,
    onChange: (Int?) -> Unit
) {
    val strings = LocalStrings.current
    Column {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "$title · ${value?.let {
                    "$it/5"
                } ?: strings.notRecorded}",
                style = MaterialTheme.typography.titleSmall
            )
            if (value == null) {
                TextButton(enabled = enabled, onClick = { onChange(3) }) { Text(strings.actionRegister) }
            }
            if (value != null) {
                TextButton(enabled = enabled, onClick = { onChange(null) }) { Text(strings.actionRemove) }
            }
        }
        Slider(
            value = (value ?: 3).toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = 1f..5f,
            steps = 3,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().semantics {
                contentDescription = title
                stateDescription = value?.let { "$it / 5" } ?: strings.notRecorded
            }
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(low, style = MaterialTheme.typography.labelSmall)
            Text(high, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun TasteProfileSummary(profile: TasteProfile) {
    val strings = LocalStrings.current
    if (profile.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(strings.flavorProfile, style = MaterialTheme.typography.titleMedium)
        profile.impression?.let { Text(it.label(strings)) }
        profile.body?.let { Text("${strings.body}: $it/5") }
        profile.acidity?.let { Text("${strings.acidity}: $it/5") }
        profile.bitterness?.let { Text("${strings.bitterness}: $it/5") }
    }
}
