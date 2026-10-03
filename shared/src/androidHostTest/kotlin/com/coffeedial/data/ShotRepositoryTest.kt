package com.coffeedial.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.coffeedial.database.CoffeeDatabase
import com.coffeedial.domain.ShotDraft
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class ShotRepositoryTest {
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
    fun invalidShotDoesNotWriteAnything() = runBlocking {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            CoffeeDatabase.Schema.create(driver)
            val repository = ShotRepository(driver)
            assertFailsWith<IllegalArgumentException> { repository.save(ShotDraft()) }
            assertTrue(repository.history.first().isEmpty())
        }
    }
}
