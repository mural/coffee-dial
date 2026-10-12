package com.coffeedial.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DrinkCupTest {
    @Test fun totalAddsTheLiquidAndSkipsEmptyLayers() {
        assertEquals(106.0, drinkTotal(36.0, 30.0, 40.0))
        assertEquals(listOf("Café", "Leche"), drinkLayers(36.0, 0.0, 40.0).map { it.label })
        assertEquals(emptyList(), drinkLayers(0.0, 0.0, 0.0))
    }

    @Test fun fillUsesCupCapacityAndKeepsTheRimEmpty() {
        assertEquals(0.95, cupFillFraction(80.0, null), 1e-9)
        assertEquals(0.0, cupFillFraction(0.0, 250.0), 1e-9)
        assertEquals(0.5, cupFillFraction(125.0, 250.0), 1e-9)
        assertEquals(0.95, cupFillFraction(250.0, 250.0), 1e-9)
        assertEquals(0.95, cupFillFraction(400.0, 150.0), 1e-9)
        assertEquals(0.95, cupFillFraction(40.0, 0.0), 1e-9)
    }

    @Test fun relationUsesCoffeeAsOne() {
        assertEquals("1 : 0.8 : 1.7", coffeeRelation(36.0, 30.0, 60.0))
        assertEquals("1 : 0 : 0", coffeeRelation(36.0, 0.0, 0.0))
        assertNull(coffeeRelation(0.0, 30.0, 10.0))
    }
}
