package com.coffeedial.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HistoryAnalysisTest {
    @Test fun emptyHistoryHasNoInventedAverages() {
        val summary = analyzeHistory(emptyList())
        assertEquals(0, summary.count)
        assertNull(summary.averageRating)
        assertNull(summary.averageRatio)
        assertEquals(0.0, summary.totalDose)
        assertEquals(0, summary.ratings.values.sum())
    }

    @Test fun averagesAndCoffeeGroupsUseAllRecords() {
        val a = Shot(
            "a", Bean("b1", "Brasil", "Tostador"), 1, 18.0, 36.0, 28.0,
            "12", null, null, null, "", 5
        )
        val b = a.copy(
            id = "b",
            bean = a.bean.copy(id = "other-device-id"),
            dose = 20.0,
            output = 60.0,
            seconds = 32.0,
            rating = 3
        )
        val summary = analyzeHistory(listOf(a, b))
        assertEquals(4.0, summary.averageRating)
        assertEquals(19.0, summary.averageDose)
        assertEquals(48.0, summary.averageOutput)
        assertEquals(30.0, summary.averageSeconds)
        assertEquals(2.5, summary.averageRatio)
        assertEquals(38.0, summary.totalDose)
        assertEquals(2, summary.coffees.single().count)
        assertEquals(1, summary.ratings[5])
        assertEquals(1, summary.ratings[3])
    }
}
