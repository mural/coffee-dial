package com.coffeedial.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.coffeedial.backup.BackupException
import com.coffeedial.backup.BackupFormat
import com.coffeedial.backup.V1_FIXTURE
import com.coffeedial.database.CoffeeDatabase
import com.coffeedial.domain.MachineDraft
import com.coffeedial.domain.ShotDraft
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class BackupRepositoryTest {
    private fun database() = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also {
        CoffeeDatabase.Schema.create(it)
    }

    @Test
    fun previewDoesNotWriteAndRepeatedImportIsIdempotent() = runBlocking {
        database().use { driver ->
            val repo = ShotRepository(driver)
            val plan = repo.prepareImport(V1_FIXTURE)
            assertEquals(1, plan.summary.newBeans)
            assertEquals(1, plan.summary.newShots)
            assertTrue(BackupFormat.decode(repo.exportBackup()).shots.isEmpty())
            repo.importBackup(plan)
            val restored = BackupFormat.decode(repo.exportBackup())
            assertEquals(BackupFormat.decode(V1_FIXTURE).shots, restored.shots)
            val repeated = repo.prepareImport(V1_FIXTURE)
            assertEquals(0, repeated.summary.newShots)
            assertEquals(1, repeated.summary.duplicates)
            repo.importBackup(repeated)
            assertEquals(1, BackupFormat.decode(repo.exportBackup()).shots.size)
        }
    }

    @Test
    fun roundTripsMilkInExportAndImport() = runBlocking {
        database().use { driver ->
            val repo = ShotRepository(driver)
            repo.save(
                ShotDraft(
                    beanName = "Flat White",
                    roaster = "Test",
                    grind = "40",
                    milk = "150",
                    extraWater = "120",
                    style = "Americano"
                )
            )
            val exported = repo.exportBackup()
            val decoded = BackupFormat.decode(exported)
            assertEquals(150.0, decoded.shots.single().milk)
            assertEquals(120.0, decoded.shots.single().extraWater)
            assertEquals("Americano", decoded.shots.single().style)
            val targetDriver = database()
            val target = ShotRepository(targetDriver)
            val plan = target.prepareImport(exported)
            target.importBackup(plan)
            val restoredShots = target.history.first()
            targetDriver.close()
            assertEquals(150.0, restoredShots.single().milk)
            assertEquals(120.0, restoredShots.single().extraWater)
            assertEquals("Americano", restoredShots.single().style)
        }
    }

    @Test
    fun roundTripsMachinesInExportAndImport() = runBlocking {
        database().use { driver ->
            val repo = ShotRepository(driver)
            repo.saveMachine(
                MachineDraft(name = "Breville Touch", type = "Espresso", year = "2023")
            )
            val exported = repo.exportBackup()
            val decoded = BackupFormat.decode(exported)
            assertEquals("Breville Touch", decoded.machines.single().name)
            val plan = repo.prepareImport(exported)
            repo.importBackup(plan)
            val restoredMachines = repo.machines.first()
            assertEquals("Breville Touch", restoredMachines.single().name)
        }
    }

    @Test
    fun existingCoffeeWithDifferentIdIsReusedWithoutDuplicatingShots() = runBlocking {
        database().use { driver ->
            val repo = ShotRepository(driver)
            repo.save(ShotDraft(beanName = "Café ☕", roaster = "Test", grind = "40"))
            val plan = repo.prepareImport(V1_FIXTURE)
            assertEquals(0, plan.summary.newBeans)
            repo.importBackup(plan)
            val backup = BackupFormat.decode(repo.exportBackup())
            assertEquals(1, backup.beans.size)
            assertEquals(2, backup.shots.size)
            assertTrue(backup.shots.all { it.beanId == backup.beans.single().id })
            assertEquals(1, repo.prepareImport(V1_FIXTURE).summary.duplicates)
        }
    }

    @Test
    fun conflictingShotNeverOverwritesLocalData() = runBlocking {
        database().use { driver ->
            val repo = ShotRepository(driver)
            repo.importBackup(repo.prepareImport(V1_FIXTURE))
            val changed = V1_FIXTURE.replace("\"rating\":4", "\"rating\":1")
            val plan = repo.prepareImport(changed)
            assertEquals(1, plan.summary.conflicts)
            assertEquals(0, plan.summary.newShots)
            repo.importBackup(plan)
            assertEquals(4, BackupFormat.decode(repo.exportBackup()).shots.single().rating)
        }
    }

    @Test
    fun conflictingBeanAlsoSkipsItsDependentShots() = runBlocking {
        database().use { driver ->
            val repo = ShotRepository(driver)
            repo.importBackup(repo.prepareImport(V1_FIXTURE))
            val changed = V1_FIXTURE.replace(
                "Café ☕",
                "Different coffee"
            ).replace("shot-1", "shot-2")
            val plan = repo.prepareImport(changed)
            assertEquals(2, plan.summary.conflicts)
            repo.importBackup(plan)
            assertEquals(1, BackupFormat.decode(repo.exportBackup()).shots.size)
        }
    }

    @Test
    fun stalePreviewIsRejectedWithoutAddingAnything() = runBlocking {
        database().use { driver ->
            val repo = ShotRepository(driver)
            val plan = repo.prepareImport(V1_FIXTURE)
            repo.save(ShotDraft(beanName = "New", grind = "20"))
            assertFailsWith<BackupException> { repo.importBackup(plan) }
            val backup = BackupFormat.decode(repo.exportBackup())
            assertEquals("New", backup.beans.single().name)
            assertEquals(1, backup.shots.size)
        }
    }

    @Test
    fun databaseFailureRollsBackAllImportedRows() = runBlocking {
        database().use { driver ->
            val repo = ShotRepository(driver)
            val plan = repo.prepareImport(V1_FIXTURE)
            driver.execute(
                null,
                "CREATE TRIGGER fail BEFORE INSERT ON shot BEGIN SELECT RAISE(ABORT, 'test'); END",
                0
            )
            assertFailsWith<Exception> { repo.importBackup(plan) }
            val backup = BackupFormat.decode(repo.exportBackup())
            assertTrue(backup.beans.isEmpty())
            assertTrue(backup.shots.isEmpty())
        }
    }

    @Test
    fun invalidFileLeavesExistingDataIntact() = runBlocking {
        database().use { driver ->
            val repo = ShotRepository(driver)
            repo.importBackup(repo.prepareImport(V1_FIXTURE))
            assertFailsWith<BackupException> { repo.prepareImport(V1_FIXTURE.dropLast(10)) }
            assertEquals(1, BackupFormat.decode(repo.exportBackup()).shots.size)
        }
    }
}
