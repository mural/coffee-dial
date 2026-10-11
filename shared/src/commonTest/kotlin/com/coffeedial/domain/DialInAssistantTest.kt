package com.coffeedial.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DialInAssistantTest {
    private val context =
        DialInContext(RoastLevel.MEDIUM, BrewMethod.ESPRESSO, "Manual", StrengthGoal.STRONGER)
    private fun shot(id: String = "one", impression: TasteImpression = TasteImpression.WEAK) = Shot(
        id, Bean("bean", "5 Hispanos", ""), 1, 16.0, 46.0, 18.0, "Medio", null, null,
        "Oster PrimaLatte", "Anterior", 3, taste = TasteProfile(impression), dialIn = context
    )

    @Test fun exampleChangesYieldOnlyAndCreatesUnevaluatedLinkedIteration() {
        val source = shot()
        val next = assertNotNull(dialInAdvice(source, context, listOf(source)).next)
        assertEquals(16.0, next.dose.toDouble())
        assertEquals(36.0, next.output.toDouble())
        assertEquals(source.grind, next.grind)
        assertEquals("", next.seconds)
        assertEquals("", next.firstDrop)
        assertEquals("", next.notes)
        assertNull(next.taste)
        assertEquals(source.id, next.dialIn?.previousShotId)
        assertEquals(false, next.dialIn?.favorite)
        assertEquals(setOf("seconds"), next.errors().keys)
    }

    @Test fun noDiagnosisWithoutTasteOrForOtherMethods() {
        assertNull(dialInAdvice(shot().copy(taste = null), context, emptyList()).next)
        assertNull(dialInAdvice(shot(), context.copy(method = BrewMethod.FILTER), emptyList()).next)
        assertNull(dialInAdvice(shot(), context.copy(roast = null), emptyList()).next)
        val balanced = shot(impression = TasteImpression.BALANCED).copy(seconds = 8.0)
        val next =
            assertNotNull(
                dialInAdvice(balanced, context.copy(goal = StrengthGoal.BALANCED), emptyList()).next
            )
        assertEquals(balanced.output, next.output.toDouble())
    }

    @Test fun additionsRequireTastingContextAndAdjustOnlyDilution() {
        val source = shot().copy(milk = 100.0, extraWater = 60.0)
        assertNull(dialInAdvice(source, context, emptyList()).next)
        val next =
            assertNotNull(
                dialInAdvice(
                    source,
                    context.copy(tasteBasis = TasteBasis.FINISHED_DRINK),
                    emptyList()
                ).next
            )
        assertEquals(48.0, next.extraWater.toDouble())
        assertEquals(100.0, next.milk.toDouble())
        assertEquals(source.output, next.output.toDouble())
        assertEquals(source.grind, next.grind)
    }

    @Test fun tasteAndRoastMatterAndGrindNumbersAreNeverInvented() {
        val sour = shot(impression = TasteImpression.TOO_SOUR)
        val light = dialInAdvice(sour, context.copy(roast = RoastLevel.LIGHT), emptyList()).next!!
        val medium = dialInAdvice(sour, context, emptyList()).next!!
        assertTrue(light.output.toDouble() > medium.output.toDouble())
        val long = sour.copy(output = 64.0, grind = "1.4.0")
        val next = dialInAdvice(long, context, emptyList()).next!!
        assertEquals(long.output, next.output.toDouble())
        assertEquals("", next.grind)
        assertTrue("grind" in next.errors())
        assertNull(dialInAdvice(long, context.copy(grinder = ""), emptyList()).next)
    }

    @Test fun learnsOnlyFromComparablePreferencesAndHandlesOrphanedOrCyclicParents() {
        val source = shot()
        val preferred = shot(
            "two",
            TasteImpression.BALANCED
        ).copy(output = 40.0, dialIn = context.copy(favorite = true))
        val second = preferred.copy(id = "three")
        val unrelated = preferred.copy(
            id = "other",
            bean = Bean("other-bean", "Otro", ""),
            output = 20.0
        )
        val advice = dialInAdvice(source, context, listOf(preferred, second, unrelated))
        assertEquals(2, advice.references.size)
        assertEquals(40.0, advice.next!!.output.toDouble())
        assertTrue(
            dialInAdvice(
                source,
                context.copy(grinder = "Otro"),
                listOf(preferred, second)
            ).references.isEmpty()
        )
        val child = source.copy(id = "child", dialIn = context.copy(previousShotId = source.id))
        assertEquals(listOf(source, child), dialInLineage(child, listOf(source, child)))
        assertEquals(listOf(child), dialInLineage(child, listOf(child)))
        val cyclic = source.copy(dialIn = context.copy(previousShotId = "child"))
        assertEquals(2, dialInLineage(child, listOf(cyclic, child)).size)
    }

    @Test fun machineIdSurvivesARenameAndRejectsAnotherMachineWithTheSameName() {
        val source = shot().copy(machine = "Bianca", machineId = "m1")
        val renamed = shot("two", TasteImpression.BALANCED).copy(
            output = 32.0,
            machine = "Lelit Bianca",
            machineId = "m1",
            dialIn = context.copy(favorite = true)
        )
        val alsoPreferred = renamed.copy(id = "three", output = 40.0)
        val sameName = renamed.copy(
            id = "other",
            machineId = "m2",
            machine = "Bianca",
            output = 24.0
        )
        val advice = dialInAdvice(source, context, listOf(renamed, alsoPreferred, sameName))
        assertEquals(setOf("two", "three"), advice.references.map { it.id }.toSet())
        assertEquals("m1", advice.next?.machineId)
        // Two matching ratios put the median at 40 g. Including the other machine would make it 32 g.
        assertEquals(40.0, advice.next?.output?.toDouble())
    }

    @Test fun grinderIdSurvivesARenameAndRejectsAnotherGrinderWithTheSameName() {
        val source = shot().copy(grinder = "Niche", grinderId = "g1")
        val renamed = shot("two", TasteImpression.BALANCED).copy(
            output = 32.0,
            grinder = "Niche Zero",
            grinderId = "g1",
            dialIn = context.copy(grinder = "otro texto", favorite = true)
        )
        val alsoPreferred = renamed.copy(id = "three", output = 40.0)
        val sameName = renamed.copy(
            id = "other",
            grinderId = "g2",
            grinder = "Niche",
            output = 24.0
        )
        val advice = dialInAdvice(source, context, listOf(renamed, alsoPreferred, sameName))
        assertEquals(setOf("two", "three"), advice.references.map { it.id }.toSet())
        assertEquals("g1", advice.next?.grinderId)
        assertEquals(40.0, advice.next?.output?.toDouble())
    }
}
