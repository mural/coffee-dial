package com.coffeedial.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class DrinkRatiosTest {
    @Test fun distinguishesMilkWaterAndPersonalMixtures() {
        assertEquals("Sin agregados", similarDrink(0.0, 0.0))
        assertEquals("Espresso macchiato", similarDrink(0.0, 0.25))
        assertEquals("Espresso macchiato", similarDrink(0.0, 0.5))
        assertEquals("Americano equilibrado", similarDrink(2.0, 0.0))
        assertEquals("Mezcla personal", similarDrink(8.0, 8.0))
        assertEquals("Mezcla personal", similarDrink(0.1, 4.0))
    }
}
