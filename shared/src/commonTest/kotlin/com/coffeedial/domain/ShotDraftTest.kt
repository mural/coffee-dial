package com.coffeedial.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShotDraftTest {
    private val valid = ShotDraft(beanName = "Colombia", grind = "42")

    @Test
    fun acceptsDecimalCommaAndOptionalTemperature() {
        assertTrue(valid.copy(dose = "18,5", temperature = "").errors().isEmpty())
        assertEquals(18.5, "18,5".decimal())
    }

    @Test
    fun rejectsInvalidAndNonFiniteMeasurements() {
        listOf("0", "-1", "NaN", "Infinity", "abc", "").forEach {
            assertTrue("dose" in valid.copy(dose = it).errors(), it)
            assertTrue("output" in valid.copy(output = it).errors(), it)
            assertTrue("seconds" in valid.copy(seconds = it).errors(), it)
        }
    }

    @Test
    fun validatesRequiredFieldsRatingAndTemperature() {
        assertTrue("beanName" in valid.copy(beanName = " ").errors())
        assertTrue("grind" in valid.copy(grind = " ").errors())
        assertTrue("rating" in valid.copy(rating = 6).errors())
        assertTrue("temperature" in valid.copy(temperature = "101").errors())
    }

    @Test
    fun validatesMilkRange() {
        assertTrue(valid.copy(milk = "").errors().isEmpty())
        assertTrue(valid.copy(milk = "60").errors().isEmpty())
        assertTrue("milk" in valid.copy(milk = "0").errors())
        assertTrue("milk" in valid.copy(milk = "201").errors())
        assertTrue("milk" in valid.copy(milk = "abc").errors())
    }

    @Test
    fun calculatesBrewRatio() {
        val shot = Shot("id", Bean("b", "Coffee", ""), 0, 18.0, 36.0, 28.0, "42", null, null, "", 4)
        assertEquals(2.0, shot.ratio)
    }
}
