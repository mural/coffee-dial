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
import com.coffeedial.ui.CoffeeButton as Button
import androidx.compose.material3.ButtonDefaults
import com.coffeedial.ui.CoffeeCard as Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.coffeedial.ui.CoffeeOutlinedButton as OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.coffeedial.ui.CoffeeTextButton as TextButton
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
    val strings = LocalStrings.current
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
                        timestamp(shot.createdAt, strings = strings),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (shot.dialIn?.favorite == true) {
                        Text(strings.favoriteLabel, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        shot.style?.takeIf { it.isNotBlank() }?.let { style ->
            item { ReadOnlyChip(strings.knownDrinkName(style)) }
        }
        item {
            DrinkCupChart(
                shot.dose,
                shot.output,
                water,
                milk,
                cup?.capacity,
                shot.milkFoam,
                canUseGenericFill = true
            )
        }
        item {
            DetailCard {
                Text(strings.detailExtraction, style = MaterialTheme.typography.titleMedium)
                MetricRow {
                    ShotMetric(
                        BeanIcon,
                        strings.doseIn,
                        "${shot.dose.pretty()} g",
                        Modifier.weight(1f)
                    )
                    ShotMetric(
                        CoffeeCup,
                        strings.outputField,
                        "${shot.output.pretty()} g",
                        Modifier.weight(1f)
                    )
                }
                if (shot.milk != null || shot.extraWater != null) {
                    MetricRow {
                        if (shot.extraWater != null) {
                            ShotMetric(
                                WaterIcon,
                                strings.addedWater,
                                "${shot.extraWater.pretty()} ml",
                                Modifier.weight(1f)
                            )
                        }
                        if (shot.milk != null) {
                            ShotMetric(
                                MilkIcon,
                                strings.milk,
                                "${shot.milk.pretty()} ml",
                                Modifier.weight(1f)
                            )
                        }
                    }
                }
                MetricRow {
                    ShotMetric(
                        MugIcon,
                        strings.totalLabel,
                        "≈ ${drinkTotal(shot.output, water, milk).pretty()}",
                        Modifier.weight(1f)
                    )
                    ShotMetric(
                        TimerIcon,
                        strings.time,
                        "${shot.seconds.pretty()} s",
                        Modifier.weight(1f)
                    )
                }
                shot.firstDrop?.takeIf { it > 0 }?.let { drop ->
                    val extraction = (shot.seconds - drop).coerceAtLeast(0.0)
                    ShotMetric(
                        TimerIcon,
                        strings.extractionPhase,
                        strings.preInfusion(drop.pretty(), extraction.pretty()),
                        Modifier.fillMaxWidth()
                    )
                }
                Text(
                    "Ratio ${strings.yield.lowercase()} 1:${shot.ratio.pretty()} · " +
                        extractionStyle(shot.ratio, strings),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    strings.waterMilkDoNotChangeRatio,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            DetailCard {
                Text(strings.grind, style = MaterialTheme.typography.titleMedium)
                val grindCanonical = grindLevels.firstOrNull { canonical ->
                    canonical.equals(shot.grind.trim(), ignoreCase = true) ||
                        strings.grindLevelLabel(canonical)
                            .equals(shot.grind.trim(), ignoreCase = true)
                }
                Text(
                    grindCanonical?.let { strings.grindLevelLabel(it) } ?: shot.grind,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                grindCanonical?.let {
                    Text(
                        strings.grindAdjustment(strings.grindLevelLabel(it)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                shot.temperature?.let {
                    LabeledValue(strings.temperatureC, "${it.pretty()} °C")
                }
            }
        }
        if (machineName(shot) != null || grinderName != null || cupName(shot) != null) {
            item {
                DetailCard {
                    Text(strings.navEquipment, style = MaterialTheme.typography.titleMedium)
                    machineName(shot)?.let { name ->
                        LabeledValue(
                            strings.machine,
                            name,
                            listOfNotNull(
                                machine?.type?.takeIf { it.isNotBlank() }
                                    ?.let { strings.machineTypeLabel(it) },
                                machine?.year?.takeIf { it.isNotBlank() }
                            ).joinToString(" · ")
                        )
                    }
                    grinderName?.let { name ->
                        LabeledValue(
                            strings.grinder,
                            name,
                            grinder?.type?.takeIf { it.isNotBlank() }
                                ?.let { strings.grinderTypeLabel(it) }
                                .orEmpty()
                        )
                    }
                    cupName(shot)?.let { name ->
                        val cupDetails = listOfNotNull(
                            cup?.weight?.let { "${it.pretty()} g" },
                            cup?.capacity?.let { "${it.pretty()} cm³" }
                        ).joinToString(" · ")
                        LabeledValue(
                            strings.cup,
                            name,
                            cupDetails
                        )
                    }
                }
            }
        }
        item {
            DetailCard {
                Text(strings.rating, style = MaterialTheme.typography.titleMedium)
                CoffeeRating(shot.rating)
                shot.taste?.let { TasteProfileSummary(it) }
                Text(strings.notes, style = MaterialTheme.typography.titleSmall)
                Text(
                    shot.notes.ifBlank { strings.notRecorded },
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
                        strings.dialContext,
                        style = MaterialTheme.typography.titleMedium
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        dial.method?.let { ReadOnlyChip(strings.prepLabel(it.label(strings))) }
                        dial.roast?.let { ReadOnlyChip(strings.roastLabel(it.label(strings))) }
                        dial.goal?.let { ReadOnlyChip(it.label(strings)) }
                        dial.tasteBasis?.let { ReadOnlyChip(it.label(strings)) }
                    }
                    if (dial.previousShotId != null) {
                        Text(
                            strings.linkedAttempt,
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
                error?.let {
                    Text(strings.userMessage(it), color = MaterialTheme.colorScheme.error)
                }
                TextButton(
                    onClick = {
                        clipboardManager.setText(
                            androidx.compose.ui.text.AnnotatedString(shotShareText(shot, strings))
                        )
                        copiedText = true
                    },
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (copiedText) strings.recipeCopied else strings.copyRecipe
                    )
                }
                onCompare?.let {
                    TextButton(
                        onClick = it,
                        enabled = !saving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.compareWithOthers)
                    }
                }
                onSaveRecipe?.let {
                    Button(
                        onClick = it,
                        enabled = !saving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.saveAsRecipe)
                    }
                }
                onDialIn?.let {
                    Button(
                        onClick = it,
                        enabled = !saving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.adjustNextExtraction)
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
                                strings.removeFavorite
                            } else {
                                strings.markFavorite
                            }
                        )
                    }
                }
                Button(onClick = onEdit, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
                    Text(strings.editShot)
                }
                if (showConfirm) {
                    Text(strings.deleteShotQuestion, color = MaterialTheme.colorScheme.error)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onDelete,
                            enabled = !saving,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.weight(1f)
                        ) { Text(strings.yesDelete) }
                        OutlinedButton(
                            onClick = { showConfirm = false },
                            enabled = !saving,
                            modifier = Modifier.weight(1f)
                        ) { Text(strings.actionCancel) }
                    }
                } else {
                    TextButton(
                        onClick = { showConfirm = true },
                        enabled = !saving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.deleteShot, color = MaterialTheme.colorScheme.error)
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
                    TextButton(onClick = { enlarged = null }) { Text(strings.actionClose) }
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
    val strings = LocalStrings.current
    val bitmap = remember(photo.id, photo.jpeg) {
        runCatching {
            photo.validate()
            photo.jpeg?.let { decodePhoto(Base64.decode(it)) }
        }.getOrNull()
    } ?: return
    Image(
        bitmap,
        contentDescription = strings.beanPhoto,
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

internal fun shotShareText(shot: Shot, strings: AppStrings = SpanishStrings): String = buildString {
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
    append(" in ").append(shot.seconds.pretty()).append("s")
    shot.firstDrop?.takeIf {
        it > 0
    }?.let { append(" (").append(strings.preinfusionText(it.pretty(), (shot.seconds - it).pretty())).append(")") }
    append("\n")
    append("⚙️ ").append(strings.grind).append(": ")
        .append(strings.grindLevelLabel(shot.grind))
    val machineStr = listOfNotNull(shot.machine, shot.grinder, shot.cup).joinToString(" · ")
    if (machineStr.isNotBlank()) append(" · ").append(machineStr)
    append("\n")
    append("⭐ ").append(strings.rating).append(": ").append(shot.rating).append("/5")
    shot.taste?.impression?.let { append(" (").append(it.label(strings)).append(")") }
    if (shot.notes.isNotBlank()) append("\n📝 ").append(shot.notes)
}
