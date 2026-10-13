package com.coffeedial.ui

import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

    @Test fun missingCapacityUsesTheMediumCupAndSmallOnesStayWider() {
        val medium = cupDrawSize(200.0)
        val generic = cupDrawSize(null)
        assertEquals(medium.width, generic.width, 0.01)
        assertEquals(168.0, generic.width, 0.01)
        assertEquals(198.0, generic.height, 0.01)
        val small = cupDrawSize(100.0)
        val linear = 0.5.pow(1.0 / 3.0)
        assertTrue(small.width > 168.0 * linear)
        assertTrue(small.height < 198.0 * linear)
        val large = cupDrawSize(300.0)
        fun volume(draw: CupDraw) = draw.width * draw.width * draw.height
        assertEquals(0.5, volume(small) / volume(medium), 0.02)
        assertEquals(1.5, volume(large) / volume(medium), 0.02)
    }

    @Test fun foamRatioSplitsTheMilkWithoutAddingVolume() {
        assertEquals(0.5, foamShare(com.coffeedial.domain.MilkFoam.HIGH), 1e-9)
        assertEquals(0.3, foamShare(com.coffeedial.domain.MilkFoam.MEDIUM), 1e-9)
        assertEquals(0.15, foamShare(com.coffeedial.domain.MilkFoam.LOW), 1e-9)
        assertEquals(0.0, foamShare(null), 1e-9)
        assertEquals("1 : 1", foamMilkRelation(0.5))
        assertEquals("1 : 2.3", foamMilkRelation(0.3))
        assertEquals("1 : 5.7", foamMilkRelation(0.15))
        assertNull(foamMilkRelation(0.0))
        assertNull(foamMilkRelation(1.0))
    }

    @Test fun relationUsesCoffeeAsOne() {
        assertEquals("1 : 0.8 : 1.7", coffeeRelation(36.0, 30.0, 60.0))
        assertEquals("1 : 0 : 0", coffeeRelation(36.0, 0.0, 0.0))
        assertNull(coffeeRelation(0.0, 30.0, 10.0))
    }
}
