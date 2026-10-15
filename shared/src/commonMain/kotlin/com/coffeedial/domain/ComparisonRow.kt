package com.coffeedial.domain

import com.coffeedial.ui.AppStrings
import com.coffeedial.ui.SpanishStrings
import com.coffeedial.ui.label
import kotlin.math.round

data class ComparisonRow(
    val label: String,
    val values: List<String>,
    val sensory: List<Int?>? = null
) {
    fun changed(index: Int): Boolean = index > 0 && values[index] != values.first()
}

/** Descriptive comparison only: time and ratio cannot establish extraction quality. */
fun compareShots(shots: List<Shot>, strings: AppStrings = SpanishStrings): List<ComparisonRow> {
    require(shots.size in 2..3 && shots.map { it.id }.distinct().size == shots.size)
    fun number(value: Double?): String =
        value?.let { (round(it * 100) / 100).toString().removeSuffix(".0") } ?: strings.notRecorded
    fun text(value: String?) = value?.takeIf { it.isNotBlank() } ?: strings.notRecorded
    fun row(label: String, value: (Shot) -> String) = ComparisonRow(label, shots.map(value))
    fun sensory(label: String, value: (Shot) -> Int?) = ComparisonRow(
        label,
        shots.map {
            value(it)?.let { n -> "$n/5" }
                ?: strings.notRecorded
        },
        shots.map(value)
    )
    return listOf(
        row(strings.beans) { it.bean.name },
        row(strings.roaster) { text(it.bean.roaster) },
        row(strings.machine) { text(it.machine) },
        row(strings.grinder) { text(it.grinder ?: it.dialIn?.grinder) },
        row(strings.grind) { strings.grindLevelLabel(it.grind) },
        row(strings.rowDose) { number(it.dose) },
        row(strings.rowYield) { number(it.output) },
        row(strings.rowRatio) { "1:${number(it.ratio)}" },
        row(strings.rowTime) { number(it.seconds) },
        row(strings.rowFirstDrop) { number(it.firstDrop) },
        row(strings.rowTemperature) { number(it.temperature) },
        row(strings.rowAddedWater) { number(it.extraWater) },
        row(strings.rowMilk) { number(it.milk) },
        row(strings.rowMilkFoam) { it.milkFoam?.label(strings) ?: strings.notRecorded },
        row(strings.styleLabel) {
            it.style?.takeIf { name -> name.isNotBlank() }?.let(strings::knownDrinkName)
                ?: strings.notRecorded
        },
        row(strings.rating) { "${it.rating}/5" },
        row(strings.flavorProfile) { it.taste?.impression?.label(strings) ?: strings.notRecorded },
        sensory(strings.body) { it.taste?.body },
        sensory(strings.acidity) { it.taste?.acidity },
        sensory(strings.bitterness) { it.taste?.bitterness },
        row(strings.notes) { text(it.notes) }
    )
}
