package com.coffeedial.ui

import com.coffeedial.domain.prepareDrinks
import kotlin.test.Test
import kotlin.test.assertEquals


class DrinkRatiosTest {
    @Test fun distinguishesMilkWaterAndPersonalMixtures() {
        assertEquals("Sin agregados", similarDrink(0.0, 0.0))
        assertEquals("Macchiato", similarDrink(0.0, 0.25))
        assertEquals("Macchiato", similarDrink(0.0, 0.5))
        assertEquals("Americano", similarDrink(2.0, 0.0))
        assertEquals("Café con leche", similarDrink(0.0, 1.5))
        assertEquals("Mezcla personal", similarDrink(8.0, 8.0))
        assertEquals("Mezcla personal", similarDrink(0.1, 4.0))
    }

    @Test fun prepareDrinksUseTheSameNamesAsTheGuide() {
        prepareDrinks.filter { it.waterParts > 0.0 || it.milkParts > 0.0 }.forEach { drink ->
            assertEquals(drink.name, similarDrink(drink.waterParts, drink.milkParts))
        }
    }

    @Test fun extractionStyleKeepsTheBoundaryOnTheShorterCup() {
        assertEquals("Ristretto", extractionStyle(1.5))
        assertEquals("Espresso", extractionStyle(1.51))
        assertEquals("Espresso", extractionStyle(2.5))
        assertEquals("Espresso largo", extractionStyle(2.51))
        assertEquals("Espresso largo", extractionStyle(3.0))
        assertEquals("Lungo", extractionStyle(3.01))
    }
}
