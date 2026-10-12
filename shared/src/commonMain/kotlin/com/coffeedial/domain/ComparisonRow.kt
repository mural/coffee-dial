package com.coffeedial.domain

import kotlin.math.round

data class ComparisonRow(
    val label: String,
    val values: List<String>,
    val sensory: List<Int?>? = null
) {
    fun changed(index: Int): Boolean = index > 0 && values[index] != values.first()
}

/** Descriptive comparison only: time and ratio cannot establish extraction quality. */
fun compareShots(shots: List<Shot>): List<ComparisonRow> {
    require(shots.size in 2..3 && shots.map { it.id }.distinct().size == shots.size)
    fun number(value: Double?): String =
        value?.let { (round(it * 100) / 100).toString().removeSuffix(".0") } ?: "Sin registrar"
    fun text(value: String?) = value?.takeIf { it.isNotBlank() } ?: "Sin registrar"
    fun row(label: String, value: (Shot) -> String) = ComparisonRow(label, shots.map(value))
    fun sensory(label: String, value: (Shot) -> Int?) = ComparisonRow(
        label,
        shots.map {
            value(it)?.let { n -> "$n/5" }
                ?: "Sin registrar"
        },
        shots.map(value)
    )
    return listOf(
        row("Café") { it.bean.name }, row("Tostador") { text(it.bean.roaster) },
        row("Máquina") {
            text(it.machine)
        },
        row("Molinillo") { text(it.grinder ?: it.dialIn?.grinder) },
        row("Molienda") { it.grind }, row("Dosis · g") { number(it.dose) },
        row("Output · g") {
            number(it.output)
        },
        row("Ratio extracción") { "1:${number(it.ratio)}" },
        row("Tiempo · s") { number(it.seconds) }, row("Primera gota · s") { number(it.firstDrop) },
        row("Temperatura · °C") {
            number(it.temperature)
        },
        row("Agua extra · ml") { number(it.extraWater) },
        row("Leche · ml") { number(it.milk) }, row("Estilo") { text(it.style) },
        row("Puntuación") { "${it.rating}/5" },
        row("Sabor") {
            when (it.taste?.impression) {
                TasteImpression.TOO_SOUR -> "Demasiado ácido"
                TasteImpression.TOO_BITTER -> "Demasiado amargo"
                TasteImpression.WEAK -> "Suave / débil"
                TasteImpression.BALANCED -> "Equilibrado"
                TasteImpression.TOO_STRONG -> "Demasiado fuerte"
                null -> "Sin registrar"
            }
        },
        sensory("Cuerpo") { it.taste?.body }, sensory("Acidez") { it.taste?.acidity },
        sensory("Amargor") { it.taste?.bitterness }, row("Notas") { text(it.notes) }
    )
}
