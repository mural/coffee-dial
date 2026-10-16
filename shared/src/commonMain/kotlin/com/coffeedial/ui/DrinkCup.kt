package com.coffeedial.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.coffeedial.ui.CoffeeTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.pow
import com.coffeedial.domain.CupProfile
import com.coffeedial.domain.MilkFoam
import org.jetbrains.compose.ui.tooling.preview.Preview

internal data class DrinkLayer(val label: String, val amount: Double)

private fun pointOnQuad(t: Float, start: Offset, control: Offset, end: Offset): Offset {
    val remain = 1f - t
    return Offset(
        remain * remain * start.x + 2f * remain * t * control.x + t * t * end.x,
        remain * remain * start.y + 2f * remain * t * control.y + t * t * end.y
    )
}

/** Liquid in the cup: extraction, added water and milk. Dose stays out of the fill. */
internal fun drinkLayers(output: Double, water: Double, milk: Double): List<DrinkLayer> = listOf(
    DrinkLayer("Café", output),
    DrinkLayer("Agua", water),
    DrinkLayer("Leche", milk)
).filter { it.amount.isFinite() && it.amount > 0.0 }

internal fun drinkTotal(output: Double, water: Double, milk: Double): Double = output + water + milk

/** The rim stays empty so the drawing never looks poured to the edge. */
internal const val CupHeadroom = 0.05

/**
 * Fraction of the cup interior filled from the bottom.
 * Without a capacity, the drink uses the cup up to the headroom.
 * With cm³, the height is the drink over that volume and still stops at the headroom.
 */
internal fun cupFillFraction(total: Double, capacityCm3: Double?): Double {
    val maxFill = 1.0 - CupHeadroom
    if (!total.isFinite() || total <= 0.0) return 0.0
    if (capacityCm3 == null || !capacityCm3.isFinite() || capacityCm3 <= 0.0) return maxFill
    return (total / capacityCm3).coerceIn(0.0, 1.0).coerceAtMost(maxFill)
}

/** Coffee in the cup is 1. Water and milk are measured against that volume. */
internal fun coffeeRelation(coffee: Double, water: Double, milk: Double): String? {
    if (!coffee.isFinite() || coffee <= 0.0) return null
    val safeWater = if (water.isFinite() && water > 0.0) water else 0.0
    val safeMilk = if (milk.isFinite() && milk > 0.0) milk else 0.0
    return "1 : ${(safeWater / coffee).pretty()} : ${(safeMilk / coffee).pretty()}"
}

/** Share of the milk drawn as foam. It stays inside the milk and adds no volume. */
internal fun foamShare(foam: MilkFoam?): Double = when (foam) {
    MilkFoam.LOW -> 0.15
    MilkFoam.MEDIUM -> 0.3
    MilkFoam.HIGH -> 0.5
    else -> 0.0
}

/** Foam : liquid milk. A cappuccino cap is about 1 : 1. */
internal fun foamMilkRelation(share: Double): String? {
    if (!share.isFinite() || share <= 0.0 || share >= 1.0) return null
    val foamToMilk = share / (1.0 - share)
    return if (foamToMilk >= 1.0) {
        "${foamToMilk.pretty()} : 1"
    } else {
        "1 : ${(1.0 / foamToMilk).pretty()}"
    }
}

internal data class CupDraw(val width: Double, val height: Double)

/**
 * Medium cup when capacity is missing. The drawing grows up to 500 cm³.
 * Smaller cups are a little wider than a plain scale.
 */
internal fun cupDrawSize(capacityCm3: Double?): CupDraw {
    val capacity = capacityCm3?.takeIf { it.isFinite() && it > 0.0 } ?: 200.0
    val volumeScale = (capacity / 200.0).coerceIn(0.4, 500.0 / 200.0)
    val linear = volumeScale.pow(1.0 / 3.0)
    val widen = if (volumeScale < 1.0) 1.0 + (1.0 - volumeScale) * 0.28 else 1.0
    return CupDraw(
        width = 168.0 * linear * widen,
        height = 198.0 * linear / (widen * widen)
    )
}

private val CupCoffee = Color(0xFF704A32)
private val CupWater = Color(0xFF6E9BB5)
private val CupMilk = Color(0xFFF6E6D4)

private fun drinkColor(label: String): Color = when (label) {
    "Agua" -> CupWater
    "Leche" -> CupMilk
    else -> CupCoffee
}

@Composable
internal fun DrinkCupChart(
    dose: Double,
    output: Double,
    water: Double,
    milk: Double,
    capacityCm3: Double? = null,
    milkFoam: MilkFoam? = null,
    onFoamClick: (() -> Unit)? = null,
    foamNote: String? = null,
    canUseGenericFill: Boolean = false
) {
    val strings = LocalStrings.current
    val layers = drinkLayers(output, water, milk)
    val total = drinkTotal(output, water, milk)
    val capacity = capacityCm3?.takeIf { it.isFinite() && it > 0.0 }
    var genericFill by rememberSaveable { mutableStateOf(false) }
    val shownCapacity = if (canUseGenericFill && genericFill) null else capacity
    val relation = coffeeRelation(output, water, milk)
    val share = foamShare(milkFoam)
    val foamRelation = if (milk > 0.0) foamMilkRelation(share) else null
    val foamMl = if (foamRelation == null) 0.0 else milk * share
    val liquidMilkMl = if (foamRelation == null) milk else milk - foamMl
    val description = buildString {
        append(
            strings.cupTotalDescription(
                total.pretty(),
                dose.pretty(),
                water.pretty(),
                milk.pretty()
            )
        )
        milkFoam?.let { append(strings.foamSpoken(it.label(strings))) }
        foamRelation?.let { append(strings.foamRatioSpoken(it)) }
        shownCapacity?.let { append(strings.capacitySpoken(it.pretty())) }
    }
    Column(
        Modifier.fillMaxWidth().semantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        milkFoam?.let { foam ->
            val foamText = strings.foamIllustrativeText(foam.label(strings), onFoamClick != null)
            if (onFoamClick != null) {
                TextButton(onClick = onFoamClick) { Text(foamText) }
            } else {
                Text(foamText, style = MaterialTheme.typography.bodySmall)
            }
            foamNote?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            foamRelation?.let { ratio ->
                Text(
                    strings.foamMilkRatioText(ratio),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    strings.foamBreakdownText(foamMl.pretty(), liquidMilkMl.pretty()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(strings.inTheCup, style = MaterialTheme.typography.titleMedium)
        if (canUseGenericFill && capacity != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = genericFill,
                    onCheckedChange = { genericFill = it }
                )
                Text(
                    strings.genericCupFill,
                    Modifier.clickable { genericFill = !genericFill },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        val draw = cupDrawSize(shownCapacity)
        Box(
            Modifier.width(draw.width.dp).height(draw.height.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            val outline = MaterialTheme.colorScheme.onSurface
            val page = MaterialTheme.colorScheme.background
            Canvas(Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val leftTop = w * 0.18f
                val rightTop = w * 0.72f
                val leftBottom = w * 0.28f
                val rightBottom = w * 0.62f
                val top = h * 0.19f
                val bottom = h * 0.88f
                val cup = Path().apply {
                    moveTo(leftTop, top)
                    lineTo(rightTop, top)
                    quadraticTo(
                        rightTop + w * 0.04f,
                        (top + bottom) / 2f,
                        rightBottom,
                        bottom
                    )
                    quadraticTo(
                        (leftBottom + rightBottom) / 2f,
                        bottom + h * 0.08f,
                        leftBottom,
                        bottom
                    )
                    quadraticTo(leftTop - w * 0.04f, (top + bottom) / 2f, leftTop, top)
                    close()
                }
                val stroke = 2.5.dp.toPx()
                val bodyShift = w / 2f - (leftTop + rightTop) / 2f
                translate(left = bodyShift) {
                    clipPath(cup) {
                        drawRect(page)
                        val liquid = layers.sumOf { it.amount }
                        val span = (bottom - top) * cupFillFraction(total, shownCapacity).toFloat()
                        if (liquid > 0.0 && span > 0f) {
                            drawRect(
                                drinkColor(layers.first().label),
                                topLeft = Offset(0f, bottom),
                                size = Size(w, h)
                            )
                            var cursor = bottom
                            layers.forEach { layer ->
                                val band = span * (layer.amount / liquid).toFloat()
                                drawRect(
                                    drinkColor(layer.label),
                                    topLeft = Offset(0f, cursor - band),
                                    size = Size(w, band + 1f)
                                )
                                cursor -= band
                            }
                            // Foam stays inside the milk band. The line marks the split.
                            if (milk > 0.0 && share > 0.0) {
                                val foamHeight = span * (milk / liquid).toFloat() * share.toFloat()
                                drawRect(
                                    Color(0xFFFFF4E5),
                                    topLeft = Offset(0f, cursor),
                                    size = Size(w, foamHeight)
                                )
                                // Small staggered bubbles, clipped to the foam rather than the milk.
                                clipRect(left = 0f, top = cursor, right = w, bottom = cursor + foamHeight) {
                                    val spacing = 7.dp.toPx()
                                    var row = 0
                                    var y = cursor + 3.dp.toPx()
                                    while (y < cursor + foamHeight) {
                                        var x = if (row % 2 == 0) 0f else spacing / 2f
                                        while (x < w) {
                                            drawCircle(
                                                Color(0xFFAA8966).copy(alpha = 0.28f),
                                                radius = 1.dp.toPx(), center = Offset(x, y),
                                                style = Stroke(width = 0.6.dp.toPx())
                                            )
                                            x += spacing
                                        }
                                        y += spacing
                                        row++
                                    }
                                }
                                val edge = 1.25.dp.toPx()
                                if (foamHeight > edge * 2f) {
                                    drawRect(
                                        Color(0xFFE6D2BA),
                                        topLeft = Offset(0f, cursor + foamHeight - edge),
                                        size = Size(w, edge)
                                    )
                                }
                            }
                        }
                    }
                    drawPath(cup, outline, style = Stroke(stroke))
                    drawOval(
                        outline,
                        topLeft = Offset(leftTop, top - 7f),
                        size = Size(rightTop - leftTop, 14f),
                        style = Stroke(stroke)
                    )
                    val wallTop = Offset(rightTop, top)
                    val wallControl = Offset(rightTop + w * 0.04f, (top + bottom) / 2f)
                    val wallBottom = Offset(rightBottom, bottom)
                    val handleStart = pointOnQuad(0.22f, wallTop, wallControl, wallBottom)
                    val handleEnd = pointOnQuad(0.58f, wallTop, wallControl, wallBottom)
                    val handle = Path().apply {
                        moveTo(handleStart.x, handleStart.y)
                        cubicTo(
                            handleStart.x + w * 0.2f,
                            handleStart.y,
                            handleEnd.x + w * 0.2f,
                            handleEnd.y,
                            handleEnd.x,
                            handleEnd.y
                        )
                    }
                    drawPath(
                        handle,
                        outline,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
            }
            Surface(
                Modifier
                    .padding(top = 1.dp)
                    .background(color = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface
            ) {
                Text(
                    "≈ ${total.pretty()}",
                    Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        relation?.let {
            Text(
                strings.coffeeWaterMilkRatio,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                it,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Row(
            Modifier.offset(y = (-4).dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CupLegend(CupCoffee, strings.legendCoffee, "${output.pretty()} g")
            CupLegend(CupWater, strings.legendWater, "${water.pretty()} ml")
            CupLegend(CupMilk, strings.legendMilk, "${liquidMilkMl.pretty()} ml")
            if (foamRelation != null) {
                CupLegend(Color(0xFFFFF4E5), strings.legendFoam, "≈ ${foamMl.pretty()} ml", outlined = true)
            }
        }
        Text(
            strings.coffeeFromDoseText(dose.pretty()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (shownCapacity != null) {
            Text(
                if (total > shownCapacity) {
                    strings.exceedsCapacityText(shownCapacity.pretty())
                } else {
                    strings.cupCapacityText(shownCapacity.pretty())
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (total > shownCapacity) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun CupLegend(
    color: Color,
    label: String,
    value: String,
    outlined: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(12.dp)) {
            drawCircle(color)
            if (outlined) {
                val inset = 0.5.dp.toPx()
                drawCircle(
                    Color(0xFFE6D2BA),
                    radius = size.minDimension / 2f - inset,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

@Preview(name = "Taza sin capacidad", showBackground = true, widthDp = 360)
@Composable
private fun DrinkCupChartNoCapacityPreview() {
    MaterialTheme {
        DrinkCupChart(dose = 18.0, output = 36.0, water = 30.0, milk = 60.0)
    }
}

@Preview(name = "Taza chica (100 cm³)", showBackground = true, widthDp = 360)
@Composable
private fun DrinkCupChartSmallPreview() {
    MaterialTheme {
        DrinkCupChart(
            dose = 18.0,
            output = 36.0,
            water = 0.0,
            milk = 36.0,
            capacityCm3 = CupProfile.SMALL.capacityCm3,
            milkFoam = MilkFoam.LOW
        )
    }
}

@Preview(name = "Macchiato", showBackground = true, widthDp = 360)
@Composable
private fun DrinkCupChartMacchiatoPreview() {
    MaterialTheme {
        DrinkCupChart(
            dose = 18.0,
            output = 36.0,
            water = 0.0,
            milk = 10.8,
            capacityCm3 = CupProfile.SMALL.capacityCm3,
            milkFoam = MilkFoam.HIGH,
            onFoamClick = {},
            foamNote = "Una cucharada de espuma firme sobre el espresso."
        )
    }
}

@Preview(name = "Taza media (200 cm³)", showBackground = true, widthDp = 360)
@Composable
private fun DrinkCupChartMediumPreview() {
    MaterialTheme {
        DrinkCupChart(
            dose = 18.0,
            output = 36.0,
            water = 0.0,
            milk = 108.0,
            capacityCm3 = CupProfile.MEDIUM.capacityCm3,
            milkFoam = MilkFoam.HIGH
        )
    }
}

@Preview(name = "Taza grande (300 cm³)", showBackground = true, widthDp = 360)
@Composable
private fun DrinkCupChartLargePreview() {
    MaterialTheme {
        DrinkCupChart(
            dose = 18.0,
            output = 36.0,
            water = 0.0,
            milk = 144.0,
            capacityCm3 = CupProfile.LARGE.capacityCm3,
            milkFoam = MilkFoam.MEDIUM
        )
    }
}

@Preview(name = "Taza extra (500 cm³)", showBackground = true, widthDp = 360)
@Composable
private fun DrinkCupChartExtraPreview() {
    MaterialTheme {
        DrinkCupChart(
            dose = 18.0,
            output = 80.0,
            water = 40.0,
            milk = 240.0,
            capacityCm3 = CupProfile.EXTRA.capacityCm3,
            milkFoam = MilkFoam.LOW
        )
    }
}

@Preview(name = "Taza a la mitad", showBackground = true, widthDp = 360)
@Composable
private fun DrinkCupChartHalfPreview() {
    MaterialTheme {
        DrinkCupChart(
            dose = 18.0,
            output = 40.0,
            water = 20.0,
            milk = 40.0,
            capacityCm3 = 200.0,
            milkFoam = MilkFoam.MEDIUM
        )
    }
}

@Preview(name = "Taza superada", showBackground = true, widthDp = 360)
@Composable
private fun DrinkCupChartOverflowPreview() {
    MaterialTheme {
        DrinkCupChart(
            dose = 18.0,
            output = 36.0,
            water = 40.0,
            milk = 120.0,
            capacityCm3 = 150.0
        )
    }
}
