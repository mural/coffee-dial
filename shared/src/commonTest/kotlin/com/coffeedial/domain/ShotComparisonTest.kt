package com.coffeedial.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShotComparisonTest {
    private val first =
        Shot(
            "one",
            Bean(
                "b",
                "Brasil",
                ""
            ),
            1, 18.0, 36.0, 28.0, "1.4.2", null, null, "Oster", "", 4
        )

    @Test
    fun preservesBaselineOrderAndHighlightsOnlyDifferences() {
        val second = first.copy(id = "two", output = 45.0, seconds = 30.0)
        val rows = compareShots(listOf(first, second))
        assertFalse(rows.first { it.label == "Dosis · g" }.changed(1))
        assertTrue(rows.first { it.label == "Output · g" }.changed(1))
        assertEquals(listOf("1:2", "1:2.5"), rows.first { it.label == "Ratio extracción" }.values)
        assertEquals(
            listOf("45", "36"),
            compareShots(listOf(second, first)).first {
                it.label ==
                    "Output · g"
            }.values
        )
    }

    @Test
    fun missingTasteIsNotZeroAndMilkDoesNotChangeExtractionRatio() {
        val second = first.copy(
            id = "two",
            milk = 100.0,
            taste = TasteProfile(body = 0, acidity = 10)
        )
        val rows = compareShots(listOf(first, second))
        assertEquals(listOf(null, 0), rows.first { it.label == "Cuerpo" }.sensory)
        assertEquals(listOf("Sin registrar", "0/10"), rows.first { it.label == "Cuerpo" }.values)
        assertFalse(rows.first { it.label == "Ratio extracción" }.changed(1))
        assertTrue(rows.first { it.label == "Leche · ml" }.changed(1))
    }

    @Test
    fun acceptsThreeUniqueShotsOnly() {
        assertEquals(
            3,
            compareShots(
                listOf(first, first.copy(id = "two"), first.copy(id = "three"))
            ).first().values.size
        )
        assertFailsWith<IllegalArgumentException> { compareShots(listOf(first)) }
        assertFailsWith<IllegalArgumentException> { compareShots(listOf(first, first)) }
        assertFailsWith<IllegalArgumentException> {
            compareShots((1..4).map { first.copy(id = it.toString()) })
        }
    }
}
