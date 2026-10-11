package com.coffeedial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeedial.domain.BrewMethod
import com.coffeedial.domain.DialInContext
import com.coffeedial.domain.RoastLevel
import com.coffeedial.domain.Shot
import com.coffeedial.domain.ShotDraft
import com.coffeedial.domain.StrengthGoal
import com.coffeedial.domain.TasteBasis
import com.coffeedial.domain.dialInAdvice
import com.coffeedial.domain.dialInLineage
import kotlinx.serialization.json.Json

private val ContextSaver = Saver<DialInContext, String>(
    save = { Json.encodeToString(it) },
    restore = { Json.decodeFromString(it) }
)

internal fun RoastLevel.label(): String = when (this) {
    RoastLevel.LIGHT -> "Claro"
    RoastLevel.MEDIUM -> "Medio"
    RoastLevel.DARK -> "Oscuro"
    RoastLevel.UNKNOWN -> "Desconocido"
}
internal fun BrewMethod.label(): String = when (this) {
    BrewMethod.ESPRESSO -> "Espresso"
    BrewMethod.FILTER -> "Filtro"
    BrewMethod.MOKA -> "Moka"
    BrewMethod.OTHER -> "Otro"
}
internal fun StrengthGoal.label(): String = when (this) {
    StrengthGoal.STRONGER -> "Más intenso"
    StrengthGoal.BALANCED -> "Mantener equilibrio"
    StrengthGoal.MILDER -> "Más suave"
}
internal fun TasteBasis.label(): String = when (this) {
    TasteBasis.ESPRESSO -> "Espresso solo"
    TasteBasis.FINISHED_DRINK -> "Bebida con agua/leche"
}

@Composable
fun DialInContextEditor(value: DialInContext, enabled: Boolean, onChange: (DialInContext) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Preparación", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BrewMethod.entries.forEach { item ->
                FilterChip(value.method == item, {
                    onChange(value.copy(method = item))
                }, enabled = enabled, label = { Text(item.label()) })
            }
        }
        Text("Tueste", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RoastLevel.entries.forEach { item ->
                FilterChip(value.roast == item, {
                    onChange(value.copy(roast = item))
                }, enabled = enabled, label = { Text(item.label()) })
            }
        }
        OutlinedTextField(
            value.grinder,
            { onChange(value.copy(grinder = it.take(200))) },
            label = {
                Text("Molinillo (opcional)")
            },
            supportingText = { Text("Ej.: 1Zpresso J-Ultra o molido de fábrica") },
            enabled = enabled,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Text("Intensidad buscada", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StrengthGoal.entries.forEach { item ->
                FilterChip(value.goal == item, {
                    onChange(value.copy(goal = item))
                }, enabled = enabled, label = { Text(item.label()) })
            }
        }
        Text("El sabor registrado corresponde a", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TasteBasis.entries.forEach { item ->
                FilterChip(value.tasteBasis == item, {
                    onChange(value.copy(tasteBasis = item))
                }, enabled = enabled, label = { Text(item.label()) })
            }
        }
    }
}

@Composable
fun DialInFormContext(value: DialInContext?, enabled: Boolean, onChange: (DialInContext?) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column {
        if (value?.previousShotId !=
            null
        ) {
            Text("Nuevo intento vinculado a una extracción anterior.")
        }
        TextButton(onClick = { expanded = !expanded }, enabled = enabled) {
            Text(
                if (expanded) "Ocultar contexto de ajuste" else "Contexto para ajustar espresso (opcional)"
            )
        }
        if (expanded) DialInContextEditor(value ?: DialInContext(), enabled) { onChange(it) }
    }
}

@Composable
fun DialInScreen(
    shot: Shot,
    history: List<Shot>,
    saving: Boolean,
    error: String?,
    onEdit: () -> Unit,
    onFavorite: (DialInContext) -> Unit,
    onSelect: (Shot) -> Unit,
    onNext: (ShotDraft, DialInContext) -> Unit
) {
    var context by rememberSaveable(shot.id, shot.dialIn, stateSaver = ContextSaver) {
        mutableStateOf(shot.dialIn ?: DialInContext())
    }
    val advice = dialInAdvice(shot, context, history)
    Column(
        Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Ajustar espresso", style = MaterialTheme.typography.headlineLarge)
        Text(shot.bean.name, style = MaterialTheme.typography.titleLarge)
        Text(
            "${shot.dose.pretty()} g → ${shot.output.pretty()} g · ${shot.seconds.pretty()} s · 1:${shot.ratio.pretty()}"
        )
        Text("Máquina: ${shot.machine ?: "Sin registrar"} · Molienda: ${shot.grind}")
        Text("Sabor: ${shot.taste?.impression?.label() ?: "Sin evaluar"}")
        TextButton(onClick = onEdit, enabled = !saving) { Text("Editar shot / evaluar sabor") }
        DialInContextEditor(context, !saving) { context = it }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Próxima extracción", style = MaterialTheme.typography.titleMedium)
                Text(advice.title, style = MaterialTheme.typography.titleLarge)
                Text(advice.explanation)
                advice.next?.let { next ->
                    Text("Dosis: ${next.dose} g · Yield: ${next.output} g")
                    if (next.milk.isNotBlank()) Text("Leche: ${next.milk} ml")
                    if (next.extraWater.isNotBlank()) Text("Agua extra: ${next.extraWater} ml")
                    if (next.grind.isBlank()) {
                        Text(
                            "Anotá el nuevo ajuste de molienda en el formulario."
                        )
                    }
                    Text("Medí el tiempo y registrá cómo salió este nuevo intento.")
                    Button(onClick = {
                        onNext(next, context)
                    }, enabled = !saving) { Text("Preparar próximo intento") }
                }
            }
        }
        Text(
            "Para comparar tiempos, medí desde la activación de la bomba.",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            "Compará el sabor con el intento anterior. El tiempo y el ratio no diagnostican por sí solos la extracción.",
            style = MaterialTheme.typography.bodySmall
        )
        TextButton(onClick = { onFavorite(context) }, enabled = !saving) {
            Text(if (shot.dialIn?.favorite == true) "Quitar favorito" else "Marcar como favorito")
        }
        if (advice.references.isNotEmpty()) {
            Text(
                "Tus referencias comparables (${advice.references.size})",
                style = MaterialTheme.typography.titleMedium
            )
            advice.references.forEach { reference ->
                TextButton(onClick = { onSelect(reference) }, enabled = !saving) {
                    Text(
                        "${reference.dose.pretty()} → ${reference.output.pretty()} g · ${reference.rating}/5${if (reference.dialIn?.favorite == true) " · Favorito" else ""}"
                    )
                }
            }
        } else {
            Text(
                "Las recetas favoritas o equilibradas con puntaje alto serán referencias cuando coincidan café, equipo y contexto."
            )
        }
        Text("Evolución", style = MaterialTheme.typography.titleMedium)
        val lineage = dialInLineage(shot, history)
        if (lineage.firstOrNull()?.dialIn?.previousShotId !=
            null
        ) {
            Text("El intento anterior ya no está disponible.")
        }
        lineage.forEachIndexed { index, item ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        "Intento ${index + 1}${if (item.dialIn?.favorite == true) " · Favorito" else ""}"
                    )
                    Text(
                        "${item.dose.pretty()} g → ${item.output.pretty()} g → ${item.seconds.pretty()} s"
                    )
                    Text("${item.taste?.impression?.label() ?: "Sin evaluar"} · ${item.rating}/5")
                    if (item.id !=
                        shot.id
                    ) {
                        TextButton(onClick = {
                            onSelect(item)
                        }, enabled = !saving) { Text("Ver intento") }
                    }
                }
            }
        }
        history.filter {
            it.dialIn?.previousShotId == shot.id
        }.sortedBy { it.createdAt }.forEach { child ->
            TextButton(onClick = { onSelect(child) }, enabled = !saving) {
                Text("Siguiente intento: ${child.output.pretty()} g · ${child.seconds.pretty()} s")
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
