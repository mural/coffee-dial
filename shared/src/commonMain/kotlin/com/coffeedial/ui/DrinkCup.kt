package com.coffeedial.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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
    capacityCm3: Double? = null
) {
    val layers = drinkLayers(output, water, milk)
    val total = drinkTotal(output, water, milk)
    val capacity = capacityCm3?.takeIf { it.isFinite() && it > 0.0 }
    val relation = coffeeRelation(output, water, milk)
    val description = buildString {
        append("Taza con total ${total.pretty()}. ")
        append("Dosis ${dose.pretty()} g, agua ${water.pretty()} ml, leche ${milk.pretty()} ml.")
        if (capacity != null) append(" Capacidad ${capacity.pretty()} cm³.")
    }
    Column(
        Modifier.fillMaxWidth().semantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("En la taza", style = MaterialTheme.typography.titleMedium)
        Box(Modifier.width(168.dp).height(198.dp), contentAlignment = Alignment.TopCenter) {
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
                        val span = (bottom - top) * cupFillFraction(total, capacity).toFloat()
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
                "Café : agua : leche",
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
            CupLegend(CupCoffee, "Café", "${output.pretty()} g")
            CupLegend(CupWater, "Agua", "${water.pretty()} ml")
            CupLegend(CupMilk, "Leche", "${milk.pretty()} ml")
        }
        Text(
            "El café de la taza sale de ${dose.pretty()} g de dosis.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (capacity != null) {
            Text(
                if (total > capacity) {
                    "Supera los ${capacity.pretty()} cm³ de la taza."
                } else {
                    "Taza de ${capacity.pretty()} cm³."
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (total > capacity) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun CupLegend(color: Color, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(12.dp)) { drawCircle(color) }
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

@Preview(name = "Taza a la mitad", showBackground = true, widthDp = 360)
@Composable
private fun DrinkCupChartHalfPreview() {
    MaterialTheme {
        DrinkCupChart(
            dose = 18.0,
            output = 40.0,
            water = 20.0,
            milk = 40.0,
            capacityCm3 = 200.0
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
