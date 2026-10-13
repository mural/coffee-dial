package com.coffeedial.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.coffeedial.database.CoffeeDatabase
import com.coffeedial.domain.Recipe
import com.coffeedial.domain.ShotDraft
import com.coffeedial.domain.asRecipe
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class ShotRepositoryTest {
    @Test fun foamPersistsAcrossReopenUpdateAndBackup() = runBlocking {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            CoffeeDatabase.Schema.create(driver)
            val repo = ShotRepository(driver)
            repo.save(ShotDraft(milk = "60", milkFoam = com.coffeedial.domain.MilkFoam.HIGH))
            val shot = ShotRepository(driver).history.first().single()
            assertEquals(com.coffeedial.domain.MilkFoam.HIGH, shot.milkFoam)
            repo.update(
                shot.id,
                ShotDraft(milk = "60", milkFoam = com.coffeedial.domain.MilkFoam.NONE)
            )
            assertEquals(
                com.coffeedial.domain.MilkFoam.NONE,
                com.coffeedial.backup.BackupFormat.decode(
                    repo.exportBackup()
                ).shots.single().milkFoam
            )
            repo.update(shot.id, ShotDraft(milk = "60"))
            assertEquals(null, repo.history.first().single().milkFoam)
        }
    }

    @Test
    fun defaultsPersistAndSyncDoesNotLoseThem() = runBlocking {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            CoffeeDatabase.Schema.create(driver)
            val repository = ShotRepository(driver)
            repository.setBrewDefault(com.coffeedial.domain.BrewDefault("machines", "m"))
            assertEquals("m", ShotRepository(driver).brewDefaults.first().single().selectedId)
            val local = repository.readSyncLocal()
            val next = local.backup.copy(
                brewDefaults = listOf(com.coffeedial.domain.BrewDefault("machines"))
            )
            repository.commitSync(local, next, "checkpoint")
            assertEquals(next.brewDefaults, repository.brewDefaults.first())
            assertEquals(
                next.brewDefaults,
                com.coffeedial.backup.BackupFormat.decode(repository.exportBackup()).brewDefaults
            )
        }
    }

    @Test
    fun persistsAcrossReopenAndReusesBeans() = runBlocking {
        val file = Files.createTempFile("coffee-dial-test", ".db")
        try {
            JdbcSqliteDriver("jdbc:sqlite:$file").use { driver ->
                CoffeeDatabase.Schema.create(driver)
                val repository = ShotRepository(driver)
                assertTrue(repository.history.first().isEmpty())
                val draft =
                    ShotDraft(
                        beanName = " Colombia ",
                        roaster = "Test",
                        grind = "42",
                        dose = "18,5",
                        notes = "Dulce",
                        rating = 4
                    )
                repository.save(draft)
                repository.save(draft.copy(temperature = "93", output = "40"))
                assertEquals(
                    1,
                    CoffeeDatabase(
                        driver
                    ).coffeeQueries.findBean("Colombia", "Test").executeAsList().size
                )
            }
            JdbcSqliteDriver("jdbc:sqlite:$file").use { driver ->
                val shots = ShotRepository(driver).history.first()
                assertEquals(2, shots.size)
                assertEquals(18.5, shots.first().dose)
                assertEquals("Dulce", shots.first().notes)
                assertEquals(4, shots.first().rating)
                assertEquals(setOf(null, 93.0), shots.map { it.temperature }.toSet())
                assertTrue(shots[0].createdAt >= shots[1].createdAt)
                assertEquals(1, shots.map { it.bean.id }.distinct().size)
            }
        } finally {
            Files.deleteIfExists(file)
        }
    }

    @Test
    fun editingPreservesShotIdentityAndCreationTime() = runBlocking {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            CoffeeDatabase.Schema.create(driver)
            val repository = ShotRepository(driver)
            repository.save(ShotDraft(beanName = "Brasil", grind = "12"))
            val before = repository.history.first().single()
            repository.update(
                before.id,
                ShotDraft(beanName = "Colombia", grind = "14", extraWater = "100", style = "Lungo")
            )
            val after = repository.history.first().single()
            assertEquals(before.id, after.id)
            assertEquals(before.createdAt, after.createdAt)
            assertEquals("14", after.grind)
            assertEquals("Colombia", after.bean.name)
            assertEquals(100.0, after.extraWater)
            assertEquals("Lungo", after.style)
        }
    }

    @Test
    fun machineEditRetainsIdentityAndDoesNotRecreateDeletedRows(): Unit = runBlocking {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            CoffeeDatabase.Schema.create(driver)
            val repo = ShotRepository(driver)
            repo.saveMachine(com.coffeedial.domain.MachineDraft("Original"))
            val id = repo.machines.first().single().id
            repo.updateMachine(id, com.coffeedial.domain.MachineDraft("Editada", "Filtro", "2025"))
            val updated = ShotRepository(driver).machines.first().single()
            assertEquals(id, updated.id)
            assertEquals("Editada", updated.name)
            assertEquals("Filtro", updated.type)
            assertEquals("2025", updated.year)
            repo.deleteMachine(id)
            assertFailsWith<IllegalStateException> {
                repo.updateMachine(id, com.coffeedial.domain.MachineDraft("No revivir"))
            }
        }
    }

    @Test
    fun grinderEditRenamesLinkedShotsAndDeleteKeepsTheName(): Unit = runBlocking {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            CoffeeDatabase.Schema.create(driver)
            val repo = ShotRepository(driver)
            repo.saveGrinder(com.coffeedial.domain.GrinderDraft("Niche"))
            val id = repo.grinders.first().single().id
            repo.save(com.coffeedial.domain.ShotDraft(grinder = "Niche", grinderId = id))
            repo.updateGrinder(id, com.coffeedial.domain.GrinderDraft("Niche Zero", "Manual"))
            val renamed = repo.history.first().single()
            assertEquals(id, renamed.grinderId)
            assertEquals("Niche Zero", renamed.grinder)
            repo.deleteGrinder(id)
            val kept = repo.history.first().single()
            assertEquals(null, kept.grinderId)
            assertEquals("Niche Zero", kept.grinder)
            assertFailsWith<IllegalStateException> {
                repo.updateGrinder(id, com.coffeedial.domain.GrinderDraft("No revivir"))
            }
        }
    }

    @Test fun recipeSurvivesDeletingTheShotThatInspiredIt() = runBlocking {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            CoffeeDatabase.Schema.create(driver)
            val repo = ShotRepository(driver)
            repo.save(ShotDraft(beanName = "Brasil", milk = "40", extraWater = "30"))
            val shot = repo.history.first().single()
            val recipe = shot.asRecipe("r1", "Diario")
            repo.saveRecipe(recipe)
            repo.delete(shot.id)
            assertEquals(emptyList(), repo.history.first())
            assertEquals(listOf(recipe), repo.recipes.first())
            val again = Recipe(
                recipe.id, "Diario fino", recipe.beanName, recipe.roaster, 20.0, 40.0,
                recipe.grind, milk = recipe.milk, extraWater = recipe.extraWater
            )
            repo.saveRecipe(again)
            assertEquals(again, repo.readSyncLocal().backup.recipes.single())
        }
    }

    @Test
    fun invalidShotDoesNotWriteAnything() = runBlocking {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            CoffeeDatabase.Schema.create(driver)
            val repository = ShotRepository(driver)
            assertFailsWith<IllegalArgumentException> { repository.save(ShotDraft(grind = "")) }
            assertTrue(repository.history.first().isEmpty())
        }
    }
}
