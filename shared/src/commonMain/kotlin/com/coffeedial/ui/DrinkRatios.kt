package com.coffeedial.ui

import kotlin.math.abs

internal data class DrinkRatioTip(val name: String, val water: Double, val milk: Double)

// Practical starting recipes, not definitions of drink styles or foam texture.
// Names of the main drinks match prepareDrinks.
internal val drinkRatioTips = listOf(
    DrinkRatioTip("Americano corto", 1.0, 0.0),
    DrinkRatioTip("Americano", 2.0, 0.0),
    DrinkRatioTip("Americano suave", 3.0, 0.0),
    DrinkRatioTip("Macchiato", 0.0, 0.3),
    DrinkRatioTip("Cortado", 0.0, 1.0),
    DrinkRatioTip("Café con leche", 0.0, 1.5),
    DrinkRatioTip("Flat white", 0.0, 2.0),
    DrinkRatioTip("Cappuccino", 0.0, 3.0),
    DrinkRatioTip("Latte", 0.0, 4.0),
    DrinkRatioTip("Latte más lechoso", 0.0, 5.0),
    DrinkRatioTip("Americano con un toque de leche", 2.0, 0.5),
    DrinkRatioTip("Agua y leche, más cremoso", 1.0, 2.0),
    DrinkRatioTip("Receta personal", 2.0, 1.0)
)

/** Same buckets as the extraction cards: the boundary belongs to the shorter style. */
internal fun extractionStyle(ratio: Double, strings: AppStrings = SpanishStrings): String {
    val name = when {
        !ratio.isFinite() || ratio <= 1.5 -> "Ristretto"
        ratio <= 2.5 -> "Espresso"
        ratio <= 3.0 -> "Espresso largo"
        else -> "Lungo"
    }
    return strings.knownDrinkName(name)
}

internal fun similarDrink(
    water: Double,
    milk: Double,
    strings: AppStrings = SpanishStrings
): String {
    val name = when {
        water == 0.0 && milk == 0.0 -> "Sin agregados"
        water == 0.0 && milk in 0.25..0.5 -> "Macchiato"
        else -> {
            val candidate = drinkRatioTips.filter {
                (it.water > 0) == (water > 0) && (it.milk > 0) == (milk > 0)
            }.minByOrNull { abs(it.water - water) + abs(it.milk - milk) }
            candidate?.takeIf {
                abs(it.water - water) <= maxOf(0.1, it.water * 0.25) &&
                    abs(it.milk - milk) <= maxOf(0.1, it.milk * 0.25)
            }?.name ?: "Mezcla personal"
        }
    }
    return strings.knownDrinkName(name)
}

/** Keeps the stored style id when the field shows its translated name. */
internal fun styleToStore(typed: String, strings: AppStrings = SpanishStrings): String {
    val known = listOf(
        "Ristretto",
        "Espresso",
        "Espresso largo",
        "Lungo",
        "Sin agregados",
        "Mezcla personal"
    ) + drinkRatioTips.map { it.name }
    return known.firstOrNull { name ->
        name == typed || strings.knownDrinkName(name) == typed
    } ?: typed
}
