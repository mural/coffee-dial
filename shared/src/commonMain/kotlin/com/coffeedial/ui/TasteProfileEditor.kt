package com.coffeedial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import kotlin.math.roundToInt

internal fun TasteImpression.label(): String = when (this) {
    TasteImpression.TOO_SOUR -> "Demasiado ácido"
    TasteImpression.TOO_BITTER -> "Demasiado amargo"
    TasteImpression.WEAK -> "Débil"
    TasteImpression.BALANCED -> "Equilibrado"
    TasteImpression.TOO_STRONG -> "Demasiado intenso"
}

@Composable
fun TasteProfileEditor(value: TasteProfile?, enabled: Boolean, onChange: (TasteProfile?) -> Unit) {
    val profile = value ?: TasteProfile()
    fun update(next: TasteProfile) {
        onChange(next.takeUnless { it.isEmpty() })
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Perfil de sabor", style = MaterialTheme.typography.titleMedium)
            Text("Opcional · ¿Cómo lo sentiste?", style = MaterialTheme.typography.bodyMedium)
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
                        Text(impression.label())
                    }
                }
            }
            if (profile.impression != null) {
                TextButton(enabled = enabled, onClick = {
                    update(profile.copy(impression = null))
                }) { Text("Quitar valoración de sabor") }
            }
            TasteSlider("Cuerpo", "Ligero", "Con mucho cuerpo", profile.body, enabled) {
                update(profile.copy(body = it))
            }
            TasteSlider("Acidez", "Baja", "Alta", profile.acidity, enabled) {
                update(profile.copy(acidity = it))
            }
            TasteSlider("Amargor", "Bajo", "Alto", profile.bitterness, enabled) {
                update(profile.copy(bitterness = it))
            }
            Text(
                "0 a 10 · Mové solo los controles que quieras registrar.",
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
    Column {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "$title · ${value?.let {
                    "$it/10"
                } ?: "Sin registrar"}",
                style = MaterialTheme.typography.titleSmall
            )
            if (value ==
                null
            ) {
                TextButton(enabled = enabled, onClick = { onChange(5) }) { Text("Registrar") }
            }
            if (value !=
                null
            ) {
                TextButton(enabled = enabled, onClick = { onChange(null) }) { Text("Quitar") }
            }
        }
        Slider(
            value = (value ?: 5).toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = 0f..10f,
            steps = 9,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().semantics {
                contentDescription = title
                stateDescription = value?.let { "$it de 10" } ?: "Sin registrar"
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
    if (profile.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Perfil de sabor", style = MaterialTheme.typography.titleMedium)
        profile.impression?.let { Text(it.label()) }
        profile.body?.let { Text("Cuerpo: $it/10") }
        profile.acidity?.let { Text("Acidez: $it/10") }
        profile.bitterness?.let { Text("Amargor: $it/10") }
    }
}
