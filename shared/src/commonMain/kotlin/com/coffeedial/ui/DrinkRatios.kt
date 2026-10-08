package com.coffeedial.ui

import kotlin.math.abs

internal data class DrinkRatioTip(val name: String, val water: Double, val milk: Double)

// Practical starting recipes, not definitions of drink styles or foam texture.
internal val drinkRatioTips = listOf(
    DrinkRatioTip("Americano corto", 1.0, 0.0),
    DrinkRatioTip("Americano equilibrado", 2.0, 0.0),
    DrinkRatioTip("Americano suave", 3.0, 0.0),
    DrinkRatioTip("Espresso macchiato", 0.0, 0.3),
    DrinkRatioTip("Cortado", 0.0, 1.0),
    DrinkRatioTip("Flat white", 0.0, 2.0),
    DrinkRatioTip("Cappuccino", 0.0, 3.0),
    DrinkRatioTip("Latte", 0.0, 4.0),
    DrinkRatioTip("Latte más lechoso", 0.0, 5.0),
    DrinkRatioTip("Americano con un toque de leche", 2.0, 0.5),
    DrinkRatioTip("Agua y leche, más cremoso", 1.0, 2.0),
    DrinkRatioTip("Receta personal", 2.0, 1.0)
)

internal fun similarDrink(water: Double, milk: Double): String {
    if (water == 0.0 && milk == 0.0) return "Sin agregados"
    if (water == 0.0 && milk in 0.25..0.5) return "Espresso macchiato"
    val candidate = drinkRatioTips.filter {
        (it.water > 0) == (water > 0) && (it.milk > 0) == (milk > 0)
    }.minByOrNull { abs(it.water - water) + abs(it.milk - milk) }
    return candidate?.takeIf {
        abs(it.water - water) <= maxOf(0.1, it.water * 0.25) &&
            abs(it.milk - milk) <= maxOf(0.1, it.milk * 0.25)
    }?.name ?: "Mezcla personal"
}
