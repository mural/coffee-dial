package com.coffeedial.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

internal data class DrinkLayer(val label: String, val amount: Double)

/** Liquid in the cup: extraction, added water and milk. Dose stays out of the fill. */
internal fun drinkLayers(output: Double, water: Double, milk: Double): List<DrinkLayer> =
    listOf(
        DrinkLayer("Café", output),
        DrinkLayer("Agua", water),
        DrinkLayer("Leche", milk)
    ).filter { it.amount.isFinite() && it.amount > 0.0 }

internal fun drinkTotal(output: Double, water: Double, milk: Double): Double =
    output + water + milk

/** Dose : extra water : milk. Dose is 1. */
internal fun doseRelation(dose: Double, water: Double, milk: Double): String? {
    if (!dose.isFinite() || dose <= 0.0) return null
    val safeWater = if (water.isFinite() && water > 0.0) water else 0.0
    val safeMilk = if (milk.isFinite() && milk > 0.0) milk else 0.0
    return "1 : ${(safeWater / dose).pretty()} : ${(safeMilk / dose).pretty()}"
}

private val CupCoffee = Color(0xFF704A32)
private val CupWater = Color(0xFF6E9BB5)
private val CupMilk = Color(0xFFF6E6D4)

@Composable
internal fun DrinkCupChart(dose: Double, output: Double, water: Double, milk: Double) {
    val layers = drinkLayers(output, water, milk)
    val total = drinkTotal(output, water, milk)
    val relation = doseRelation(dose, water, milk)
    val description = buildString {
        append("Taza con total ${total.pretty()}. ")
        append("Dosis ${dose.pretty()} g, agua ${water.pretty()} ml, leche ${milk.pretty()} ml.")
    }
    Column(
        Modifier.fillMaxWidth().semantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("En la taza", style = MaterialTheme.typography.titleMedium)
        Box(Modifier.width(168.dp).height(188.dp), contentAlignment = Alignment.TopCenter) {
            val outline = MaterialTheme.colorScheme.onSurface
            val ceramic = MaterialTheme.colorScheme.surfaceContainerHigh
            Canvas(Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val leftTop = w * 0.18f
                val rightTop = w * 0.72f
                val leftBottom = w * 0.28f
                val rightBottom = w * 0.62f
                val top = h * 0.16f
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
                val stroke = 2.dp.toPx()
                clipPath(cup) {
                    drawRect(ceramic)
                    if (total > 0.0) {
                        var cursor = bottom
                        layers.forEach { layer ->
                            val band = ((bottom - top) * (layer.amount / total)).toFloat()
                            drawRect(
                                when (layer.label) {
                                    "Agua" -> CupWater
                                    "Leche" -> CupMilk
                                    else -> CupCoffee
                                },
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
                val handle = Path().apply {
                    moveTo(rightTop - 2f, top + 16f)
                    cubicTo(
                        w * 0.96f,
                        top + 8f,
                        w * 0.96f,
                        top + (bottom - top) * 0.55f,
                        rightBottom + 4f,
                        top + (bottom - top) * 0.62f
                    )
                }
                drawPath(
                    handle,
                    outline,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
            Surface(
                Modifier.padding(top = 4.dp),
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
                "Dosis : agua : leche",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                it,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CupLegend(CupCoffee, "Café", "${output.pretty()} g")
            CupLegend(CupWater, "Agua", "${water.pretty()} ml")
            CupLegend(CupMilk, "Leche", "${milk.pretty()} ml")
        }
        Text(
            "El café de la taza sale de ${dose.pretty()} g de dosis.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
