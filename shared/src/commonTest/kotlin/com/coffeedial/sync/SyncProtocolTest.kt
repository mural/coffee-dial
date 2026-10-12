package com.coffeedial.sync

import com.coffeedial.backup.BackupBeanV1
import com.coffeedial.backup.BackupFormat
import com.coffeedial.backup.BackupMachineV1
import com.coffeedial.backup.BackupShotV1
import com.coffeedial.data.SnapshotRepository
import com.coffeedial.data.SnapshotStore
import com.coffeedial.domain.Recipe
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
        assertEquals(
            edited.shots,
            mergeSync(base, base, SyncDocument(backup = edited)).backup.shots
        )
        assertEquals(
            edited.shots,
            mergeSync(base, edited, SyncDocument(backup = base)).backup.shots
        )
        assertEquals(
            edited.shots,
            mergeSync(base, edited, SyncDocument(backup = edited)).backup.shots
        )
        val conflict = base.copy(shots = base.shots.map { it.copy(notes = "other") })
        val clash = mergeSync(base, edited, SyncDocument(revision = 4, backup = conflict))
        assertEquals(1, clash.conflicts.size)
        assertEquals("s", clash.conflicts.single().id)
        assertEquals(edited.shots, clash.backup.shots)
    }

    @Test fun resolutionKeepsTheChosenCopyAndAcknowledgesTheRemoteBase() {
        val base = backup()
        val local = base.copy(shots = base.shots.map { it.copy(notes = "local") })
        val remoteBackup = base.copy(shots = base.shots.map { it.copy(notes = "remote") })
        val remote = SyncDocument(revision = 4, backup = remoteBackup)
        val conflict = mergeSync(base, local, remote).conflicts.single()
        val saved = SyncCheckpoint("email:a@b.c", SyncDocument(backup = base))
        val keepMine = planResolution(local, saved, remote, conflict, true, "email:a@b.c")
        assertEquals("local", keepMine.backup.shots.single().notes)
        assertEquals("remote", keepMine.checkpoint.document.backup.shots.single().notes)
        val merged = mergeSync(
            keepMine.checkpoint.document.backup,
            keepMine.backup,
            remote,
            keepMine.checkpoint.revived.toSet()
        )
        assertTrue(merged.conflicts.isEmpty())
        assertEquals("local", merged.backup.shots.single().notes)
        val keepTheirs = planResolution(local, saved, remote, conflict, false, "email:a@b.c")
        assertEquals("remote", keepTheirs.backup.shots.single().notes)
        assertTrue(
            mergeSync(
                keepTheirs.checkpoint.document.backup,
                keepTheirs.backup,
                remote
            ).conflicts.isEmpty()
        )
    }

    @Test fun keepingALocalEditAgainstARemoteDeleteRevivesThatRecord() {
        val base = backup()
        val local = base.copy(shots = base.shots.map { it.copy(notes = "local") })
        val remote = SyncDocument(
            revision = 2,
            backup = base.copy(shots = emptyList()),
            deleted = mapOf("shots" to listOf("s"))
        )
        val conflict = mergeSync(base, local, remote).conflicts.single()
        val planned = planResolution(
            local,
            SyncCheckpoint("a", SyncDocument(backup = base)),
            remote,
            conflict,
            true,
            "a"
        )
        assertEquals(listOf("shots:s"), planned.checkpoint.revived)
        val again = mergeSync(
            planned.checkpoint.document.backup,
            planned.backup,
            remote,
            planned.checkpoint.revived.toSet()
        )
        assertTrue(again.conflicts.isEmpty())
        assertEquals("local", again.backup.shots.single().notes)
    }

    @Test fun fillingEquipmentIdsIsNotAConflictButDifferentIdsAre() {
        val machine = BackupMachineV1("m1", "Bianca", "Espresso", "")
        val other = BackupMachineV1("m2", "Bianca", "Espresso", "")
        val base = backup().copy(machines = listOf(machine, other))
        val named = base.shots.single().copy(machine = "Bianca")
        val linked = named.copy(machineId = "m1")
        val local = base.copy(shots = listOf(linked))
        val remote = base.copy(shots = listOf(named))
        val filled = mergeSync(base, local, SyncDocument(backup = remote))
        assertTrue(filled.conflicts.isEmpty())
        assertEquals("m1", filled.backup.shots.single().machineId)
        val diverged = mergeSync(
            base,
            local,
            SyncDocument(backup = base.copy(shots = listOf(named.copy(machineId = "m2"))))
        )
        assertEquals(1, diverged.conflicts.size)
        val grinder = com.coffeedial.backup.BackupGrinderV1("g1", "Niche", "Eléctrico")
        val otherGrinder = grinder.copy(id = "g2")
        val withGrinders = base.copy(grinders = listOf(grinder, otherGrinder))
        val linkedGrinder = withGrinders.shots.single().copy(grinder = "Niche", grinderId = "g1")
        val namedGrinder = linkedGrinder.copy(grinderId = null)
        val filledGrinder = mergeSync(
            withGrinders,
            withGrinders.copy(shots = listOf(linkedGrinder)),
            SyncDocument(backup = withGrinders.copy(shots = listOf(namedGrinder)))
        )
        assertTrue(filledGrinder.conflicts.isEmpty())
        assertEquals("g1", filledGrinder.backup.shots.single().grinderId)
        val divergedGrinder = mergeSync(
            withGrinders,
            withGrinders.copy(shots = listOf(linkedGrinder)),
            SyncDocument(
                backup = withGrinders.copy(
                    shots = listOf(linkedGrinder.copy(grinderId = "g2"))
                )
            )
        )
        assertEquals(1, divergedGrinder.conflicts.size)
    }

    @Test fun recipeEditsMergeWhenTheyDifferAndConflictOnTheSameRecord() {
        val recipe = Recipe(
            "r",
            "Diario",
            "Brasil",
            "",
            18.0,
            36.0,
            "Medio"
        )
        val base = backup()
        val added = mergeSync(
            base,
            base.copy(recipes = listOf(recipe)),
            SyncDocument(backup = base)
        )
        assertTrue(added.conflicts.isEmpty())
        assertEquals(listOf(recipe), added.backup.recipes)
        val diverged = mergeSync(
            base.copy(recipes = listOf(recipe)),
            base.copy(recipes = listOf(recipe.copy(name = "Local"))),
            SyncDocument(backup = base.copy(recipes = listOf(recipe.copy(grind = "Fino"))))
        )
        assertEquals(listOf("recipes"), diverged.conflicts.map { it.kind })
        assertEquals("Local", diverged.backup.recipes.single().name)
    }

    @Test fun deletionPropagatesButEditVersusDeleteConflicts() {
        val base = backup()
        val removed = base.copy(shots = emptyList())
        val remote = SyncDocument(backup = removed, deleted = mapOf("shots" to listOf("s")))
        assertTrue(mergeSync(base, base, remote).backup.shots.isEmpty())
        assertTrue(mergeSync(base, removed, SyncDocument(backup = base)).backup.shots.isEmpty())
        val edited = base.copy(shots = base.shots.map { it.copy(notes = "edited") })
        assertEquals(1, mergeSync(base, edited, remote).conflicts.size)
        assertEquals(1, mergeSync(removed, base, remote).conflicts.size)
    }

    @Test fun differentRecordsMergeAndEmptyCloudDoesNotDeleteUnsyncedData() {
        val base = backup()
        val local = base.copy(shots = base.shots + base.shots.single().copy(id = "new"))
        val remote = base.copy(shots = base.shots.map { it.copy(notes = "remote") })
        val merged = mergeSync(base, local, SyncDocument(backup = remote))
        assertEquals(2, merged.backup.shots.size)
        assertEquals("remote", merged.backup.shots.single { it.id == "s" }.notes)
        val empty = BackupFormat.create(emptyList(), emptyList())
        assertEquals(base.shots, mergeSync(empty, base, SyncDocument(backup = empty)).backup.shots)
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
