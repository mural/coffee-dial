package com.coffeedial.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PrepareDrinkTest {
    private fun drink(name: String) = prepareDrinks.first { it.name == name }

    @Test fun oneTimesEspressoIsTheReferenceShot() {
        val amounts = drink("Espresso").amounts(1.0)
        assertEquals(18.0, amounts?.dose)
        assertEquals(36.0, amounts?.output)
        assertEquals(0.0, amounts?.water)
        assertEquals(0.0, amounts?.milk)
        assertEquals("Espresso", amounts?.style)
        assertNull(amounts?.milkFoam)
    }

    @Test fun lungoIsALongerExtractionAndMacchiatoCarriesFoam() {
        val lungo = drink("Lungo").amounts(1.0)
        assertEquals(63.0, lungo?.output)
        assertEquals(0.0, lungo?.milk)
        val macchiato = drink("Macchiato").amounts(1.0)
        assertEquals(10.8, macchiato?.milk)
        assertEquals(MilkFoam.HIGH, macchiato?.milkFoam)
        assertEquals(54.0, drink("Café con leche").amounts(1.0)?.milk)
        assertEquals(MilkFoam.LOW, drink("Café con leche").amounts(1.0)?.milkFoam)
    }

    @Test fun americanoWaterIsTwiceTheCoffeeAndHalfSizeScalesEverything() {
        val full = drink("Americano").amounts(1.0)
        assertEquals(72.0, full?.water)
        assertEquals(0.0, full?.milk)
        val half = drink("Americano").amounts(0.5)
        assertEquals(9.0, half?.dose)
        assertEquals(18.0, half?.output)
        assertEquals(36.0, half?.water)
    }

    @Test fun largerAmericanoKeepsTheExtractionAndTheAddedWater() {
        val amounts = drink("Americano").amounts(1.5)
        assertEquals(27.0, amounts?.dose)
        assertEquals(54.0, amounts?.output)
        assertEquals(108.0, amounts?.water)
    }

    @Test fun cupFollowsTheDrinkVolume() {
        val espresso = drink("Espresso").amounts(2.0)!!
        val espressoVolume = espresso.output + espresso.water + espresso.milk
        assertEquals(CupProfile.SMALL, cupProfileFor(espressoVolume))
        val americano = drink("Americano").amounts(1.0)
        assertEquals(108.0, americano!!.output + americano.water)
        assertEquals(CupProfile.MEDIUM, cupProfileFor(108.0))
        assertEquals(CupProfile.SMALL, cupProfileFor(100.0))
        assertEquals(CupProfile.MEDIUM, cupProfileFor(100.1))
        assertEquals(CupProfile.MEDIUM, cupProfileFor(200.0))
        assertEquals(CupProfile.LARGE, cupProfileFor(200.1))
        assertEquals(CupProfile.LARGE, cupProfileFor(300.0))
        assertEquals(CupProfile.EXTRA, cupProfileFor(300.1))
        assertEquals(CupProfile.EXTRA, cupProfileFor(360.0))
        assertEquals(CupProfile.EXTRA, cupProfileFor(500.0))
        assertEquals(500.0, cupProfileFor(640.0).capacityCm3)
    }

    @Test fun latteStopsBeforeTheMilkWouldExceedTheForm() {
        val latte = drink("Latte")
        assertEquals(1.3, latte.maxSize())
        assertEquals(187.2, latte.amounts(1.3)?.milk)
        assertNull(latte.amounts(1.4))
        assertEquals(2.0, drink("Espresso").maxSize())
    }

    @Test fun doseStepsAreWholeGramsAroundTheReference() {
        val espresso = drink("Espresso")
        assertEquals(9..36, espresso.doseChoices())
        val rounded = espresso.amountsForDose(32)
        assertEquals(32.0, rounded?.dose)
        assertEquals(64.0, rounded?.output)
        assertEquals(18.0, espresso.amountsForDose(18)?.dose)
        assertEquals(36.0, espresso.amountsForDose(18)?.output)
    }

    @Test fun milkDrinksStopOnTheLastWholeGramThatFits() {
        val latte = drink("Latte")
        assertEquals(9..25, latte.doseChoices())
        assertEquals(200.0, latte.amountsForDose(25)?.milk)
        assertNull(latte.amountsForDose(26))
        assertEquals(9..33, drink("Cappuccino").doseChoices())
        assertEquals(198.0, drink("Cappuccino").amountsForDose(33)?.milk)
        assertNull(drink("Cappuccino").amountsForDose(34))
    }
}
