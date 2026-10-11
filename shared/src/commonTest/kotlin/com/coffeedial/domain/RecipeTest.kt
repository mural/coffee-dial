package com.coffeedial.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecipeTest {
    @Test fun doseKeepsTheRatioAndTheOtherEditsRecalculateOneSide() {
        val start = RecipeAmounts(18.0, 36.0)
        val dosed = start.change(RatioEdit.DOSE, 20.0)
        assertEquals(20.0, dosed?.dose)
        assertEquals(40.0, dosed?.output)
        val scaled = start.change(RatioEdit.RATIO, 2.5)
        assertEquals(18.0, scaled?.dose)
        assertEquals(45.0, scaled?.output)
        val yielded = start.change(RatioEdit.OUTPUT, 40.0)
        assertEquals(18.0, yielded?.dose)
        assertEquals(40.0, yielded?.output)
        assertNull(start.change(RatioEdit.DOSE, 0.0))
        assertNull(start.change(RatioEdit.OUTPUT, 1001.0))
    }

    @Test fun savedRecipeDropsTimingAndStaysAfterTheShotIsGone() {
        val shot = Shot(
            id = "s", bean = Bean("b", "Brasil", "Origen"), createdAt = 1,
            dose = 18.0, output = 36.0, seconds = 28.0, grind = "42",
            temperature = 93.0, milk = 40.0, machine = "Bianca", notes = "Dulce", rating = 5,
            extraWater = 30.0, style = "Americano", cup = "Cerámica", firstDrop = 8.0,
            taste = TasteProfile(TasteImpression.BALANCED),
            dialIn = DialInContext(grinder = "Manual"), grinder = "Niche"
        )
        val recipe = shot.asRecipe("r", "Diario")
        assertEquals("Niche", recipe.grinder)
        assertEquals(40.0, recipe.milk)
        assertEquals(30.0, recipe.extraWater)
        assertTrue(recipe.isValid())
        val draft = recipe.toDraft()
        assertEquals("", draft.seconds)
        assertEquals("", draft.firstDrop)
        assertEquals("", draft.notes)
        assertNull(draft.taste)
        assertNull(draft.dialIn)
        assertEquals("18.0", draft.dose)
        assertEquals("36.0", draft.output)
    }
}
