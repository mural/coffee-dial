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

    @Test fun relationUsesDoseAsOne() {
        assertEquals("1 : 1 : 2", doseRelation(18.0, 18.0, 36.0))
        assertEquals("1 : 0 : 0", doseRelation(18.0, 0.0, 0.0))
        assertNull(doseRelation(0.0, 10.0, 10.0))
    }
}
