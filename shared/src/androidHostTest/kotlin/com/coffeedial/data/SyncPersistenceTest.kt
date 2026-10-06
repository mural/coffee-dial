package com.coffeedial.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.coffeedial.database.CoffeeDatabase
import com.coffeedial.domain.ShotDraft
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class SyncPersistenceTest {
    @Test fun failedCommitRollsBackDataAndCheckpointTogether() = runBlocking {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            CoffeeDatabase.Schema.create(driver)
            val repo = ShotRepository(driver)
            repo.save(ShotDraft(grind = "12"))
            val before = repo.readSyncLocal()
            driver.execute(
                null,
                "CREATE TRIGGER fail_sync BEFORE INSERT ON shot BEGIN SELECT RAISE(ABORT, 'simulated disk failure'); END",
                0
            )
            assertFails { repo.commitSync(before, before.backup, "next") }
            assertEquals(before.backup.shots, repo.readSyncLocal().backup.shots)
            assertNull(repo.readSyncLocal().checkpoint)
            driver.execute(null, "DROP TRIGGER fail_sync", 0)
            repo.commitSync(before, before.backup, "next")
            assertEquals("next", repo.readSyncLocal().checkpoint)
            repo.update(before.backup.shots.single().id, ShotDraft(grind = "14"))
            assertFails { repo.commitSync(before, before.backup, "stale") }
            assertEquals("14", repo.readSyncLocal().backup.shots.single().grind)
        }
    }

    @Test fun versionFourMigrationPreservesIdsAndAllowsDistinctBeansWithSameName() = runBlocking {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            driver.execute(null, "PRAGMA foreign_keys = ON", 0)
            driver.execute(
                null,
                "CREATE TABLE bean (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, roaster TEXT NOT NULL, UNIQUE(name, roaster))",
                0
            )
            driver.execute(
                null,
                "CREATE TABLE machine (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, type TEXT NOT NULL, year TEXT NOT NULL)",
                0
            )
            driver.execute(
                null,
                "CREATE TABLE shot (id TEXT NOT NULL PRIMARY KEY, bean_id TEXT NOT NULL REFERENCES bean(id), created_at INTEGER NOT NULL, dose REAL NOT NULL, output REAL NOT NULL, seconds REAL NOT NULL, grind TEXT NOT NULL, temperature REAL, notes TEXT NOT NULL, rating INTEGER NOT NULL, milk REAL, machine TEXT)",
                0
            )
            driver.execute(null, "CREATE INDEX shot_created_at ON shot(created_at DESC)", 0)
            driver.execute(null, "CREATE INDEX shot_bean_id ON shot(bean_id)", 0)
            driver.execute(null, "INSERT INTO bean VALUES ('b', 'Brasil', '')", 0)
            driver.execute(
                null,
                "INSERT INTO shot VALUES ('s', 'b', 100, 18, 36, 28, '12', NULL, 'original', 4, NULL, NULL)",
                0
            )
            CoffeeDatabase(driver).transaction {
                CoffeeDatabase.Schema.migrate(driver, 4, CoffeeDatabase.Schema.version)
            }
            val repo = ShotRepository(driver)
            val shot = repo.readSyncLocal().backup.shots.single()
            assertEquals("s", shot.id)
            assertEquals("b", shot.beanId)
            assertEquals("original", shot.notes)
            assertNull(shot.extraWater)
            assertNull(shot.style)
            repo.update(
                shot.id,
                ShotDraft(beanName = "Brasil", extraWater = "150", style = "Americano")
            )
            assertEquals(150.0, repo.readSyncLocal().backup.shots.single().extraWater)
            driver.execute(
                null,
                "INSERT INTO bean (id, name, roaster) VALUES ('b2', 'Brasil', '')",
                0
            )
            assertEquals(2, repo.readSyncLocal().backup.beans.size)
        }
    }

    @Test fun deletingBeanKeepsSqlHistoryAndBackupWithoutRestoringCatalogEntry() = runBlocking {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            CoffeeDatabase.Schema.create(driver)
            driver.execute(null, "PRAGMA foreign_keys = ON", 0)
            val repo = ShotRepository(driver)
            val draft = ShotDraft(beanName = "Brasil")
            repo.save(draft)
            val shot = repo.history.first().single()
            repo.deleteBean(shot.bean.id)
            assertEquals(emptyList(), repo.beans.first())
            assertEquals(shot, repo.history.first().single())
            repo.update(shot.id, draft.copy(notes = "Editado"))
            assertEquals(emptyList(), repo.beans.first())
            val backup = repo.exportBackup()
            repo.replaceWithBackup(backup)
            assertEquals(emptyList(), repo.beans.first())
            assertEquals(shot.bean, repo.history.first().single().bean)
            assertEquals("Editado", repo.history.first().single().notes)
        }
    }
}
