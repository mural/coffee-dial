package com.coffeedial.domain

import com.coffeedial.backup.BackupFormat
import com.coffeedial.data.SnapshotRepository
import com.coffeedial.data.SnapshotStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ManualAmountTest {
    @Test
    fun largeManualAmountsSurviveBackupAndRecipe() = runTest {
        val store = object : SnapshotStore {
            var value: String? = null
            override suspend fun read() = value
            override suspend fun compareAndSet(expected: String?, next: String): Boolean {
                if (value != expected) return false
                value = next
                return true
            }
        }
        val repo = SnapshotRepository.open(store)
        repo.save(ShotDraft(milk = "999", extraWater = "257"))
        val backup = BackupFormat.decode(repo.exportBackup())
        assertEquals(999.0, backup.shots.single().milk)
        assertEquals(257.0, backup.shots.single().extraWater)
        val recipe = repo.history.value.single().asRecipe("recipe", "Manual")
        val restored = BackupFormat.decode(BackupFormat.encode(backup.copy(recipes = listOf(recipe))))
        assertEquals(999.0, restored.recipes.single().milk)
    }
}
