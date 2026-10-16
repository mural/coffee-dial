package com.coffeedial.domain

import com.coffeedial.data.SnapshotRepository
import com.coffeedial.data.SnapshotStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class GrindSettingTest {
    @Test
    fun proNotationDoesNotInterpretDecimalsOrFreeTextAsTriplets() {
        assertEquals(ProGrindSetting(1, 4, 2), ProGrindSetting.parse(" 1.4.2 "))
        assertEquals("0.0.0", ProGrindSetting.parse("0.0.0")?.notation())
        val invalid = listOf(
            "Medio", "18 clics", "2.3", "1..2", ".4.2", "1.4.",
            "-1.4.2", "10000.0.0", "1.2.3.4"
        )
        invalid.forEach {
            assertNull(ProGrindSetting.parse(it), it)
        }
    }

    @Test
    fun explicitModesPreserveReferencesWithoutGuessingLegacyDecimals() {
        assertEquals(GrindAdjustmentMode.FREE, GrindAdjustmentMode.detect("2.3"))
        assertEquals(GrindAdjustmentMode.TURNS, GrindAdjustmentMode.detect("1.4.2"))
        listOf(GrindAdjustmentMode.CLICKS, GrindAdjustmentMode.STEPLESS, GrindAdjustmentMode.MACRO_MICRO).forEach {
            val saved = it.notation("12,5 / referencia A")
            assertEquals(it, GrindAdjustmentMode.detect(saved))
            assertEquals("12,5 / referencia A", it.value(saved))
            assertEquals("", it.notation(""))
            assertEquals("", it.value("Medio"))
        }
    }

    private class Store : SnapshotStore {
        var value: String? = null
        override suspend fun read() = value
        override suspend fun compareAndSet(expected: String?, next: String): Boolean {
            if (value != expected) return false
            value = next
            return true
        }
    }

    @Test
    fun presetsAndLegacyAdjustmentsSurviveReopenBackupAndEdit() = runTest {
        val store = Store()
        val repo = SnapshotRepository.open(store)
        val values = grindLevels + listOf("1.4.2", "18 clics", "2.3", "mi ajuste anterior", "Stepless: 12,5", "Macro/micro: 5 / E", "Clicks: 18")
        values.forEach { repo.save(ShotDraft(grind = it)) }
        val reopened = SnapshotRepository.open(store)
        assertEquals(values.sorted(), reopened.history.value.map { it.grind }.sorted())
        val restored = SnapshotRepository.open(Store())
        restored.importBackup(restored.prepareImport(reopened.exportBackup()))
        assertEquals(values.sorted(), restored.history.value.map { it.grind }.sorted())
        val shot = restored.history.value.first { it.grind == "1.4.2" }
        restored.update(shot.id, shot.toDraft().copy(grind = "Fino"))
        assertEquals("Fino", restored.history.value.first { it.id == shot.id }.grind)
    }
}
