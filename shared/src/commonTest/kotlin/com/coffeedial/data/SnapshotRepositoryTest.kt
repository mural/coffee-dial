package com.coffeedial.data

import com.coffeedial.backup.BackupException
import com.coffeedial.backup.BackupFormat
import com.coffeedial.domain.MachineDraft
import com.coffeedial.domain.ShotDraft
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class SnapshotRepositoryTest {
    private class Store : SnapshotStore {
        var text: String? = null
        var fail = false
        var conflict = false
        override suspend fun read() = text
        override suspend fun compareAndSet(expected: String?, next: String): Boolean {
            check(!fail)
            if (conflict || text != expected) return false
            text = next
            return true
        }
    }

    @Test
    fun reopenAndBackupRoundTripPreserveData() = runTest {
        val store = Store()
        val first = SnapshotRepository.open(store)
        first.save(ShotDraft(beanName = "Brasil", grind = "12", milk = "80", notes = "Chocolate"))
        first.saveMachine(MachineDraft("Bambino"))
        val reopened = SnapshotRepository.open(store)
        assertEquals(first.history.value, reopened.history.value)
        assertEquals(first.machines.value, reopened.machines.value)
        val target = SnapshotRepository.open(Store())
        val backup = reopened.exportBackup()
        target.importBackup(target.prepareImport(backup))
        assertEquals(reopened.history.value, target.history.value)
        assertEquals(1, target.prepareImport(backup).summary.duplicates)
        val shot = reopened.history.value.single()
        reopened.update(shot.id, ShotDraft(grind = "14"))
        assertEquals(shot.id, reopened.history.value.single().id)
        assertEquals(shot.createdAt, reopened.history.value.single().createdAt)
        reopened.delete(shot.id)
        assertTrue(SnapshotRepository.open(store).history.value.isEmpty())
    }

    @Test
    fun machineEditKeepsIdAndSurvivesReopen() = runTest {
        val store = Store()
        val repo = SnapshotRepository.open(store)
        repo.saveMachine(MachineDraft("Original"))
        val id = repo.machines.value.single().id
        repo.updateMachine(id, MachineDraft("Editada", "Filtro", "2025"))
        val machine = SnapshotRepository.open(store).machines.value.single()
        assertEquals(id, machine.id)
        assertEquals("Editada", machine.name)
        assertEquals("Filtro", machine.type)
        assertEquals("2025", machine.year)
        repo.deleteMachine(id)
        assertFailsWith<IllegalStateException> {
            repo.updateMachine(id, MachineDraft("No revivir"))
        }
    }

    @Test
    fun failedWriteNeverPublishesUnsavedData() = runTest {
        val store = Store()
        val repo = SnapshotRepository.open(store)
        store.fail = true
        assertFailsWith<IllegalStateException> { repo.save(ShotDraft(grind = "12")) }
        assertTrue(repo.history.value.isEmpty())
        assertEquals(null, store.text)
        store.fail = false
        store.conflict = true
        assertFailsWith<BackupException> { repo.save(ShotDraft(grind = "12")) }
        assertTrue(repo.history.value.isEmpty())
    }

    @Test
    fun otherTabChangesInvalidateImportPreviewWithoutOverwriting() = runTest {
        val store = Store()
        val a = SnapshotRepository.open(store)
        val b = SnapshotRepository.open(store)
        val source = SnapshotRepository.open(Store())
        source.save(ShotDraft(grind = "10"))
        val preview = a.prepareImport(source.exportBackup())
        b.save(ShotDraft(grind = "20"))
        assertFailsWith<BackupException> { a.importBackup(preview) }
        assertEquals("20", a.history.value.single().grind)
        a.save(ShotDraft(grind = "30"))
        assertEquals(2, SnapshotRepository.open(store).history.value.size)
    }

    @Test
    fun futureOrCorruptStorageIsNotReset() = runTest {
        val store = Store()
        val future = BackupFormat.encode(BackupFormat.create(emptyList(), emptyList()))
            .replace("\"schemaVersion\": 1", "\"schemaVersion\": 999")
        store.text = future
        assertFailsWith<BackupException> { SnapshotRepository.open(store) }
        assertEquals(future, store.text)
        store.text = "broken"
        assertFailsWith<BackupException> { SnapshotRepository.open(store) }
        assertEquals("broken", store.text)
    }
}
