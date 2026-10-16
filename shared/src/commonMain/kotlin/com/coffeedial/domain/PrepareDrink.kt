package com.coffeedial.domain

import com.coffeedial.ui.AppStrings
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.roundToInt

/** A starting drink. Water and milk are parts of the coffee in the cup, not of the dry dose. */
data class PrepareDrink(
    val name: String,
    val style: String,
    val extractionRatio: Double,
    val waterParts: Double,
    val milkParts: Double,
    val milkFoam: MilkFoam? = null
)

data class PreparedAmounts(
    val dose: Double,
    val output: Double,
    val water: Double,
    val milk: Double,
    val style: String,
    val milkFoam: MilkFoam? = null
)

const val PrepareBaseDose = 18.0

enum class CupProfile(val label: String, val capacityCm3: Double) {
    SMALL("Chica", 100.0),
    MEDIUM("Media", 200.0),
    LARGE("Grande", 300.0),
    EXTRA("Extra", 500.0);

    fun label(strings: AppStrings): String = when (this) {
        SMALL -> strings.cupSmall
        MEDIUM -> strings.cupMedium
        LARGE -> strings.cupLarge
        EXTRA -> strings.cupExtra
    }
}

/** Smallest cup that holds the drink. Only the 500 cm³ cup can still overflow. */
fun cupProfileFor(volume: Double): CupProfile {
    val safe = if (volume.isFinite() && volume > 0.0) volume else 0.0
    return CupProfile.entries.firstOrNull { it.capacityCm3 >= safe } ?: CupProfile.EXTRA
}

val prepareDrinks = listOf(
    PrepareDrink("Espresso", "Espresso", 2.0, 0.0, 0.0),
    PrepareDrink("Lungo", "Lungo", 3.5, 0.0, 0.0),
    PrepareDrink("Americano", "Americano", 2.0, 2.0, 0.0),
    PrepareDrink("Macchiato", "Macchiato", 2.0, 0.0, 0.3, MilkFoam.HIGH),
    PrepareDrink("Cortado", "Cortado", 2.0, 0.0, 1.0, MilkFoam.LOW),
    PrepareDrink("Café con leche", "Café con leche", 2.0, 0.0, 1.5, MilkFoam.LOW),
    PrepareDrink("Flat white", "Flat white", 2.0, 0.0, 2.0, MilkFoam.LOW),
    PrepareDrink("Cappuccino", "Cappuccino", 2.0, 0.0, 3.0, MilkFoam.HIGH),
    PrepareDrink("Latte", "Latte", 2.0, 0.0, 4.0, MilkFoam.MEDIUM)
)

private fun round2(value: Double) = round(value * 100) / 100

/** Scales dose, coffee, water and milk together. Returns null when a field would not save. */
fun PrepareDrink.amounts(
    size: Double,
    baseDose: Double = PrepareBaseDose,
    foam: MilkFoam? = milkFoam
): PreparedAmounts? {
    if (!size.isFinite() || size <= 0.0 || !baseDose.isFinite() || baseDose <= 0.0) return null
    if (!extractionRatio.isFinite() || extractionRatio <= 0.0) return null
    val dose = round2(baseDose * size)
    val output = round2(dose * extractionRatio)
    val water = round2(output * waterParts)
    val milk = round2(output * milkParts)
    if (dose !in 0.1..100.0 || output !in 0.1..1000.0) return null
    if (waterParts > 0.0 && water !in 1.0..1000.0) return null
    if (milkParts > 0.0 && milk !in 1.0..200.0) return null
    val recordedFoam = if (milk > 0.0) foam else null
    return PreparedAmounts(dose, output, water, milk, style, recordedFoam)
}

/**
 * Whole-gram dose. Other amounts scale from this number, so 32 g stays 32 g
 * instead of 18 g × 1.8 = 32.4 g.
 */
fun PrepareDrink.amountsForDose(
    doseGrams: Int,
    foam: MilkFoam? = milkFoam
): PreparedAmounts? {
    if (doseGrams <= 0 || !extractionRatio.isFinite() || extractionRatio <= 0.0) return null
    val dose = doseGrams.toDouble()
    val output = round2(dose * extractionRatio)
    val water = round2(output * waterParts)
    val milk = round2(output * milkParts)
    if (dose !in 0.1..100.0 || output !in 0.1..1000.0) return null
    if (waterParts > 0.0 && water !in 1.0..1000.0) return null
    if (milkParts > 0.0 && milk !in 1.0..200.0) return null
    val recordedFoam = if (milk > 0.0) foam else null
    return PreparedAmounts(dose, output, water, milk, style, recordedFoam)
}

/** Half to double the reference dose, in whole grams that still fit the form. */
fun PrepareDrink.doseChoices(baseDose: Double = PrepareBaseDose): IntRange {
    val reference = baseDose.roundToInt().coerceAtLeast(1)
    var low = ceil(baseDose * 0.5 - 1e-9).toInt().coerceAtLeast(1)
    var high = floor(baseDose * 2.0 + 1e-9).toInt()
    if (high < low) high = low
    while (low < reference && amountsForDose(low) == null) low++
    while (high > reference && amountsForDose(high) == null) high--
    return if (low > high) reference..reference else low..high
}

/** Largest 0.1 step from 0.5 to 2 that still fits the shot form. */
fun PrepareDrink.maxSize(baseDose: Double = PrepareBaseDose): Double {
    val perCup = baseDose * extractionRatio
    val caps = mutableListOf(2.0, 100.0 / baseDose, 1000.0 / perCup)
    if (waterParts > 0.0) caps += 1000.0 / (perCup * waterParts)
    if (milkParts > 0.0) caps += 200.0 / (perCup * milkParts)
    val stepped = floor((caps.min() * 10.0) + 1e-9) / 10.0
    return stepped.coerceIn(0.5, 2.0)
}
