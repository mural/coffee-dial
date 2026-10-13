package com.coffeedial.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.coffeedial.domain.Bean
import com.coffeedial.domain.BrewMethod
import com.coffeedial.domain.Cup
import com.coffeedial.domain.DialInContext
import com.coffeedial.domain.Grinder
import com.coffeedial.domain.Machine
import com.coffeedial.domain.RoastLevel
import com.coffeedial.domain.Shot
import com.coffeedial.domain.StrengthGoal
import com.coffeedial.domain.TasteBasis
import com.coffeedial.domain.grindLevels
import com.coffeedial.photos.BeanPhoto
import com.coffeedial.photos.decodePhoto
import kotlin.io.encoding.Base64
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ShotDetail(
    shot: Shot,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    saving: Boolean,
    onCompare: (() -> Unit)? = null,
    onSaveRecipe: (() -> Unit)? = null,
    onDialIn: (() -> Unit)? = null,
    onFavorite: (() -> Unit)? = null,
    error: String? = null,
    beans: List<Bean> = emptyList(),
    machines: List<Machine> = emptyList(),
    grinders: List<Grinder> = emptyList(),
    cups: List<Cup> = emptyList()
) {
    var showConfirm by remember { mutableStateOf(false) }
    var enlarged by remember { mutableStateOf<BeanPhoto?>(null) }
    var copiedText by remember { mutableStateOf(false) }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val beanPhoto = shot.bean.photo?.takeIf { !it.jpeg.isNullOrBlank() }
        ?: beans.firstOrNull { it.id == shot.bean.id }?.photo?.takeIf { !it.jpeg.isNullOrBlank() }
    val machine = linked(shot.machineId, shot.machine, machines, { it.id }, { it.name })
    val grinder = linked(shot.grinderId, shot.grinder, grinders, { it.id }, { it.name })
    val cup = linked(shot.cupId, shot.cup, cups, { it.id }, { it.name })
    val grinderName = shot.grinder?.takeIf { it.isNotBlank() }
        ?: shot.dialIn?.grinder?.takeIf { it.isNotBlank() }
    val water = shot.extraWater ?: 0.0
    val milk = shot.milk ?: 0.0
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (beanPhoto != null) {
                    BeanThumbnail(beanPhoto, 72.dp) { enlarged = beanPhoto }
                } else {
                    Surface(
                        Modifier.size(72.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                BeanIcon,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(shot.bean.name, style = MaterialTheme.typography.headlineSmall)
                    if (shot.bean.roaster.isNotBlank()) {
                        Text(
                            shot.bean.roaster,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        timestamp(shot.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (shot.dialIn?.favorite == true) {
                        Text("Favorito", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        shot.style?.takeIf { it.isNotBlank() }?.let { style ->
            item { ReadOnlyChip(style) }
        }
        item {
            DrinkCupChart(shot.dose, shot.output, water, milk, cup?.capacity, shot.milkFoam)
        }
        item {
            DetailCard {
                Text("Extracción", style = MaterialTheme.typography.titleMedium)
                MetricRow {
                    ShotMetric(
                        BeanIcon,
                        "Dosis de entrada",
                        "${shot.dose.pretty()} g",
                        Modifier.weight(1f)
                    )
                    ShotMetric(
                        CoffeeCup,
                        "Output",
                        "${shot.output.pretty()} g",
                        Modifier.weight(1f)
                    )
                }
                if (shot.milk != null || shot.extraWater != null) {
                    MetricRow {
                        if (shot.extraWater != null) {
                            ShotMetric(
                                WaterIcon,
                                "Agua extra",
                                "${shot.extraWater.pretty()} ml",
                                Modifier.weight(1f)
                            )
                        }
                        if (shot.milk != null) {
                            ShotMetric(
                                MilkIcon,
                                "Leche",
                                "${shot.milk.pretty()} ml",
                                Modifier.weight(1f)
                            )
                        }
                    }
                }
                MetricRow {
                    ShotMetric(
                        MugIcon,
                        "Total",
                        "≈ ${drinkTotal(shot.output, water, milk).pretty()}",
                        Modifier.weight(1f)
                    )
                    ShotMetric(
                        TimerIcon,
                        "Tiempo",
                        "${shot.seconds.pretty()} s",
                        Modifier.weight(1f)
                    )
                }
                shot.firstDrop?.takeIf { it > 0 }?.let { drop ->
                    val extraction = (shot.seconds - drop).coerceAtLeast(0.0)
                    ShotMetric(
                        TimerIcon,
                        "Fase de extracción",
                        "Pre-infusión: ${drop.pretty()} s · Extracción: ${extraction.pretty()} s",
                        Modifier.fillMaxWidth()
                    )
                }
                val ratioCategory = when {
                    shot.ratio <= 1.5 -> "ristretto"
                    shot.ratio <= 2.5 -> "espresso"
                    shot.ratio <= 3.0 -> "espresso largo"
                    else -> "lungo"
                }
                Text(
                    "Ratio output 1:${shot.ratio.pretty()} · $ratioCategory",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    "El agua y la leche no cambian el ratio de extracción.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            DetailCard {
                Text("Molienda", style = MaterialTheme.typography.titleMedium)
                Text(
                    shot.grind,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                grindLevels.firstOrNull { it.equals(shot.grind.trim(), ignoreCase = true) }
                    ?.let {
                        Text(
                            "Ajuste del molino · $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                shot.temperature?.let {
                    LabeledValue("Temperatura · °C", "${it.pretty()} °C")
                }
            }
        }
        if (machineName(shot) != null || grinderName != null || cupName(shot) != null) {
            item {
                DetailCard {
                    Text("Equipo", style = MaterialTheme.typography.titleMedium)
                    machineName(shot)?.let { name ->
                        LabeledValue(
                            "Máquina",
                            name,
                            listOfNotNull(
                                machine?.type?.takeIf { it.isNotBlank() },
                                machine?.year?.takeIf { it.isNotBlank() }
                            ).joinToString(" · ")
                        )
                    }
                    grinderName?.let { name ->
                        LabeledValue(
                            "Molinillo",
                            name,
                            grinder?.type?.takeIf { it.isNotBlank() }.orEmpty()
                        )
                    }
                    cupName(shot)?.let { name ->
                        val cupDetails = listOfNotNull(
                            cup?.weight?.let { "${it.pretty()} g" },
                            cup?.capacity?.let { "${it.pretty()} cm³" }
                        ).joinToString(" · ")
                        LabeledValue(
                            "Taza",
                            name,
                            cupDetails
                        )
                    }
                }
            }
        }
        item {
            DetailCard {
                Text("¿Qué tal salió?", style = MaterialTheme.typography.titleMedium)
                CoffeeRating(shot.rating)
                shot.taste?.let { TasteProfileSummary(it) }
                Text("Notas", style = MaterialTheme.typography.titleSmall)
                Text(
                    shot.notes.ifBlank { "Sin notas" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (shot.notes.isBlank()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
        shot.dialIn?.takeIf { it.hasVisibleContext() }?.let { dial ->
            item {
                DetailCard {
                    Text(
                        "Contexto para ajustar espresso",
                        style = MaterialTheme.typography.titleMedium
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        dial.method?.let { ReadOnlyChip("Preparación · ${it.label()}") }
                        dial.roast?.let { ReadOnlyChip("Tueste · ${it.label()}") }
                        dial.goal?.let { ReadOnlyChip(it.label()) }
                        dial.tasteBasis?.let { ReadOnlyChip(it.label()) }
                    }
                    if (dial.previousShotId != null) {
                        Text(
                            "Nuevo intento vinculado a una extracción anterior.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                TextButton(
                    onClick = {
                        clipboardManager.setText(
                            androidx.compose.ui.text.AnnotatedString(shotShareText(shot))
                        )
                        copiedText = true
                    },
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (copiedText) "¡Receta copiada al portapapeles! 📋" else "Copiar receta / resumen"
                    )
                }
                onCompare?.let {
                    TextButton(
                        onClick = it,
                        enabled = !saving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Comparar con otros intentos")
                    }
                }
                onSaveRecipe?.let {
                    Button(
                        onClick = it,
                        enabled = !saving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Guardar como receta")
                    }
                }
                onDialIn?.let {
                    Button(
                        onClick = it,
                        enabled = !saving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ajustar próxima extracción")
                    }
                }
                onFavorite?.let {
                    TextButton(
                        onClick = it,
                        enabled = !saving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (shot.dialIn?.favorite == true) {
                                "Quitar favorito"
                            } else {
                                "Marcar favorito"
                            }
                        )
                    }
                }
                Button(onClick = onEdit, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
                    Text("Editar shot")
                }
                if (showConfirm) {
                    Text("¿Querés eliminar este shot?", color = MaterialTheme.colorScheme.error)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onDelete,
                            enabled = !saving,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.weight(1f)
                        ) { Text("Sí, eliminar") }
                        OutlinedButton(
                            onClick = { showConfirm = false },
                            enabled = !saving,
                            modifier = Modifier.weight(1f)
                        ) { Text("Cancelar") }
                    }
                } else {
                    TextButton(
                        onClick = { showConfirm = true },
                        enabled = !saving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Eliminar shot", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
    enlarged?.let { image ->
        Dialog(onDismissRequest = { enlarged = null }) {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BeanThumbnail(image, 280.dp)
                    TextButton(onClick = { enlarged = null }) { Text("Cerrar") }
                }
            }
        }
    }
}

private fun machineName(shot: Shot) = shot.machine?.takeIf { it.isNotBlank() }

private fun cupName(shot: Shot) = shot.cup?.takeIf { it.isNotBlank() }

private fun DialInContext.hasVisibleContext(): Boolean =
    roast != null || method != null || goal != null || tasteBasis != null || previousShotId != null

private fun <T> linked(
    id: String?,
    name: String?,
    items: List<T>,
    itemId: (T) -> String,
    itemName: (T) -> String
): T? {
    items.firstOrNull { id != null && itemId(it) == id }?.let { return it }
    val key = name?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return null
    return items.filter { itemName(it).trim().lowercase() == key }.distinctBy(itemId).singleOrNull()
}

@Composable
private fun DetailCard(content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = { content() }
        )
    }
}

@Composable
private fun MetricRow(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        content()
    }
}

@Composable
private fun LabeledValue(label: String, value: String, detail: String = "") {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.titleMedium)
        if (detail.isNotBlank()) {
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReadOnlyChip(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text,
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun BeanThumbnail(photo: BeanPhoto, size: Dp, onClick: (() -> Unit)? = null) {
    val bitmap = remember(photo.id, photo.jpeg) {
        runCatching {
            photo.validate()
            photo.jpeg?.let { decodePhoto(Base64.decode(it)) }
        }.getOrNull()
    } ?: return
    Image(
        bitmap,
        contentDescription = "Foto del café",
        modifier = Modifier.size(size).clip(RoundedCornerShape(12.dp)).then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        ),
        contentScale = ContentScale.Crop
    )
}

@Preview(name = "Detalle del Shot", showBackground = true, widthDp = 400, heightDp = 1400)
@Composable
private fun ShotDetailPreview() {
    MaterialTheme {
        ShotDetail(
            shot = mockShot.copy(
                style = "Latte",
                extraWater = 30.0,
                cup = "Cerámica",
                cupId = "c",
                grinder = "Niche Zero",
                grinderId = "g1",
                machineId = "1",
                firstDrop = 8.0,
                dialIn = DialInContext(
                    roast = RoastLevel.MEDIUM,
                    method = BrewMethod.ESPRESSO,
                    goal = StrengthGoal.BALANCED,
                    tasteBasis = TasteBasis.FINISHED_DRINK,
                    favorite = true
                )
            ),
            onEdit = {},
            onDelete = {},
            saving = false,
            machines = mockMachines,
            grinders = mockGrinders,
            cups = listOf(Cup("c", "Cerámica", 180.0, 250.0))
        )
    }
}

internal fun shotShareText(shot: Shot): String = buildString {
    append("☕ ").append(shot.bean.name)
    if (shot.bean.roaster.isNotBlank()) append(" (").append(shot.bean.roaster).append(")")
    val originProcess = listOfNotNull(
        shot.bean.origin?.takeIf { it.isNotBlank() },
        shot.bean.process?.takeIf { it.isNotBlank() }
    ).joinToString(" · ")
    if (originProcess.isNotBlank()) append(" - ").append(originProcess)
    append("\n")
    append(
        "⏱️ "
    ).append(
        shot.dose.pretty()
    ).append(
        " g in -> "
    ).append(shot.output.pretty()).append(" g out (1:").append(shot.ratio.pretty()).append(")")
    append(" en ").append(shot.seconds.pretty()).append("s")
    shot.firstDrop?.takeIf {
        it > 0
    }?.let { append(" (Pre-infusión: ").append(it.pretty()).append("s)") }
    append("\n")
    append("⚙️ Molienda: ").append(shot.grind)
    val machineStr = listOfNotNull(shot.machine, shot.grinder, shot.cup).joinToString(" · ")
    if (machineStr.isNotBlank()) append(" · ").append(machineStr)
    append("\n")
    append("⭐ Rating: ").append(shot.rating).append("/5")
    shot.taste?.impression?.let { append(" (").append(it.label()).append(")") }
    if (shot.notes.isNotBlank()) append("\n📝 ").append(shot.notes)
}
