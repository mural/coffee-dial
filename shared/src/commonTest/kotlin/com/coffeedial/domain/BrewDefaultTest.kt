package com.coffeedial.domain

import com.coffeedial.backup.BackupException
import com.coffeedial.backup.BackupFormat
import com.coffeedial.sync.SyncCheckpoint
import com.coffeedial.sync.SyncDocument
import com.coffeedial.sync.mergeSync
import com.coffeedial.sync.planResolution
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BrewDefaultTest {
    @Test
    fun fillsByIdUsingCurrentNamesAndIgnoresDeletedItems() {
        val defaults =
            listOf(
                BrewDefault("beans", "b"),
                BrewDefault("machines", "m"),
                BrewDefault("grinders", "g"),
                BrewDefault("cups", "c")
            )
        val draft = defaults.newShot(
            listOf(Bean("b", "Brasil", "Tostador")),
            listOf(Machine("m", "Renombrada", "Espresso", "")),
            listOf(Grinder("g", "J-Ultra", "Manual")),
            listOf(Cup("c", "Grande"))
        )
        assertEquals("Brasil", draft.beanName)
        assertEquals("Tostador", draft.roaster)
        assertEquals("Renombrada", draft.machine)
        assertEquals("m", draft.machineId)
        assertEquals("g", draft.grinderId)
        assertEquals("c", draft.cupId)
        assertEquals(
            ShotDraft(),
            defaults.newShot(emptyList(), emptyList(), emptyList(), emptyList())
        )
    }

    @Test
    fun oldBackupsHaveNoDefaultsAndNewOnesRejectInvalidOrMislabeledData() {
        val old = BackupFormat.create(emptyList(), emptyList()).copy(schemaVersion = 11)
        assertTrue(BackupFormat.decode(BackupFormat.encode(old)).brewDefaults.isEmpty())
        val next = old.copy(schemaVersion = 12, brewDefaults = listOf(BrewDefault("beans", "b")))
        assertEquals(next.brewDefaults, BackupFormat.decode(BackupFormat.encode(next)).brewDefaults)
        assertFailsWith<BackupException> { BackupFormat.encode(next.copy(schemaVersion = 11)) }
        assertFailsWith<BackupException> {
            BackupFormat.encode(next.copy(brewDefaults = listOf(BrewDefault("bad", "b"))))
        }
        assertFailsWith<BackupException> {
            BackupFormat.encode(
                next.copy(
                    brewDefaults =
                        next.brewDefaults + next.brewDefaults
                )
            )
        }
    }

    @Test
    fun differentCategoriesMergeButConcurrentChoicesRequireResolution() {
        val base = BackupFormat.create(emptyList(), emptyList())
        val local = base.copy(brewDefaults = listOf(BrewDefault("beans", "a")))
        val remote =
            SyncDocument(backup = base.copy(brewDefaults = listOf(BrewDefault("cups", "c"))))
        val merged = mergeSync(base, local, remote)
        assertTrue(merged.conflicts.isEmpty())
        assertEquals(2, merged.backup.brewDefaults.size)
        val other = remote.copy(
            backup = base.copy(brewDefaults = listOf(BrewDefault("beans", "b")))
        )
        val conflict = mergeSync(base, local, other).conflicts.single()
        val resolved =
            planResolution(
                local,
                SyncCheckpoint("test", SyncDocument(backup = base)),
                other,
                conflict,
                false,
                "test"
            )
        assertEquals(other.backup.brewDefaults, resolved.backup.brewDefaults)
        val unset = local.copy(brewDefaults = listOf(BrewDefault("beans")))
        assertEquals(
            unset.brewDefaults,
            mergeSync(local, unset, SyncDocument(backup = local)).backup.brewDefaults
        )
    }
}
