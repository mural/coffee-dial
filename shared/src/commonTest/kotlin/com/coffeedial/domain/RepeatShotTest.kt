package com.coffeedial.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RepeatShotTest {
    @Test
    fun repeatsRecipeWithoutCopyingEvaluationOrIdentity() {
        val shot = Shot(
            "old-shot", Bean("bean", "Brasil", "Tostador"), 100L,
            16.0, 32.0, 28.0, "1.4.2", 93.0, 40.0, "Máquina", "Amargo", 5,
            extraWater = 10.0, style = "Latte", cup = "Taza", firstDrop = 6.0,
            taste = TasteProfile(body = 8), dialIn = DialInContext(favorite = true),
            machineId = "machine", cupId = "cup", grinder = "J-Ultra", grinderId = "grinder"
        )
        val draft = shot.repeatDraft()
        assertEquals("Brasil", draft.beanName)
        assertEquals("16.0", draft.dose)
        assertEquals("32.0", draft.output)
        assertEquals("1.4.2", draft.grind)
        assertEquals("40.0", draft.milk)
        assertEquals("10.0", draft.extraWater)
        assertEquals("machine", draft.machineId)
        assertEquals("cup", draft.cupId)
        assertEquals("grinder", draft.grinderId)
        assertEquals("", draft.seconds)
        assertEquals("", draft.firstDrop)
        assertEquals("", draft.notes)
        assertEquals(ShotDraft().rating, draft.rating)
        assertNull(draft.taste)
        assertNull(draft.dialIn)
    }
}
