package com.coffeedial.domain

import com.coffeedial.backup.BackupException
import com.coffeedial.backup.BackupFormat
import com.coffeedial.backup.V1_FIXTURE
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MilkFoamTest {
    @Test fun backupAndRecipePreserveEveryChoiceAndLegacyStaysUnknown() {
        val old = BackupFormat.decode(V1_FIXTURE)
        assertNull(old.shots.single().milkFoam)
        for (foam in MilkFoam.entries) {
            val backup = old.copy(shots = listOf(old.shots.single().copy(milkFoam = foam)))
            assertEquals(
                foam,
                BackupFormat.decode(BackupFormat.encode(backup)).shots.single().milkFoam
            )
            assertFailsWith<BackupException> {
                BackupFormat.encode(backup.copy(schemaVersion = 14))
            }
            val shot =
                Shot(
                    "shot",
                    Bean(
                        "b",
                        "Brasil",
                        ""
                    ),
                    1, 18.0, 36.0, 25.0, "Fino", null, 60.0, null, "", 3, milkFoam = foam
                )
            assertEquals(foam, shot.toDraft().milkFoam)
            assertEquals(foam, shot.repeatDraft().milkFoam)
            val recipe = shot.asRecipe("r", "Receta")
            assertEquals(foam, recipe.toDraft().milkFoam)
            assertEquals(2.0, shot.ratio)
            assertEquals(60.0, shot.milk)
            val withRecipe = old.copy(recipes = listOf(recipe))
            assertEquals(
                recipe,
                BackupFormat.decode(BackupFormat.encode(withRecipe)).recipes.single()
            )
            assertFailsWith<BackupException> {
                BackupFormat.encode(withRecipe.copy(schemaVersion = 14))
            }
        }
    }

    @Test fun comparisonDistinguishesUnrecordedFromNoFoam() {
        val shot =
            Shot("s", Bean("b", "Brasil", ""), 1, 18.0, 36.0, 25.0, "Fino", null, null, null, "", 3)
        val rows = compareShots(listOf(shot, shot.copy(id = "s2", milkFoam = MilkFoam.NONE)))
        val foam = rows.first { it.label == "Espuma" }
        assertEquals(listOf("Sin registrar", "Sin espuma"), foam.values)
        assertTrue(foam.changed(1))
        val wire = BackupFormat.encode(
            BackupFormat.decode(V1_FIXTURE).let {
                it.copy(shots = listOf(it.shots.single().copy(milkFoam = MilkFoam.HIGH)))
            }
        )
        assertFailsWith<BackupException> { BackupFormat.decode(wire.replace("HIGH", "UNKNOWN")) }
    }
}
