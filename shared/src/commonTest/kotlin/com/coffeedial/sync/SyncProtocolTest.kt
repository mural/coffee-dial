package com.coffeedial.sync

import com.coffeedial.backup.BackupBeanV1
import com.coffeedial.backup.BackupFormat
import com.coffeedial.backup.BackupShotV1
import com.coffeedial.data.SnapshotRepository
import com.coffeedial.data.SnapshotStore
import com.coffeedial.domain.ShotDraft
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class SyncProtocolTest {
    private fun backup() = BackupFormat.create(
        listOf(BackupBeanV1("b", "Brasil", "")),
        listOf(BackupShotV1("s", "b", 1, 18.0, 36.0, 28.0, "12", null, notes = "", rating = 4))
    )

    @Test fun remoteEditAndLocalEditAreDistinguished() {
        val base = backup()
        val edited = base.copy(shots = base.shots.map { it.copy(notes = "edited") })
        assertEquals(edited.shots, mergeSync(base, base, SyncDocument(backup = edited)).shots)
        assertEquals(edited.shots, mergeSync(base, edited, SyncDocument(backup = base)).shots)
        assertEquals(edited.shots, mergeSync(base, edited, SyncDocument(backup = edited)).shots)
        val conflict = base.copy(shots = base.shots.map { it.copy(notes = "other") })
        assertFailsWith<IllegalStateException> {
            mergeSync(base, edited, SyncDocument(backup = conflict))
        }
    }

    @Test fun deletionPropagatesButEditVersusDeleteConflicts() {
        val base = backup()
        val removed = base.copy(shots = emptyList())
        val remote = SyncDocument(backup = removed, deleted = mapOf("shots" to listOf("s")))
        assertTrue(mergeSync(base, base, remote).shots.isEmpty())
        assertTrue(mergeSync(base, removed, SyncDocument(backup = base)).shots.isEmpty())
        val edited = base.copy(shots = base.shots.map { it.copy(notes = "edited") })
        assertFailsWith<IllegalStateException> { mergeSync(base, edited, remote) }
        assertFailsWith<IllegalStateException> { mergeSync(removed, base, remote) }
    }

    @Test fun differentRecordsMergeAndEmptyCloudDoesNotDeleteUnsyncedData() {
        val base = backup()
        val local = base.copy(shots = base.shots + base.shots.single().copy(id = "new"))
        val remote = base.copy(shots = base.shots.map { it.copy(notes = "remote") })
        val merged = mergeSync(base, local, SyncDocument(backup = remote))
        assertEquals(2, merged.shots.size)
        assertEquals("remote", merged.shots.single { it.id == "s" }.notes)
        val empty = BackupFormat.create(emptyList(), emptyList())
        assertEquals(base.shots, mergeSync(empty, base, SyncDocument(backup = empty)).shots)
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

    @Test fun checkpointAndDataCommitTogetherAndConcurrentEditsSurvive() = runTest {
        val store = Store()
        val repo = SnapshotRepository.open(store)
        repo.save(ShotDraft(grind = "12"))
        val expected = repo.readSyncLocal()
        repo.update(expected.backup.shots.single().id, ShotDraft(grind = "14"))
        assertFailsWith<IllegalStateException> {
            repo.commitSync(expected, expected.backup, "checkpoint")
        }
        val updated = repo.readSyncLocal()
        assertNull(updated.checkpoint)
        assertEquals("14", updated.backup.shots.single().grind)
        repo.commitSync(updated, updated.backup, "checkpoint")
        assertEquals("checkpoint", SnapshotRepository.open(store).readSyncLocal().checkpoint)
    }
}
