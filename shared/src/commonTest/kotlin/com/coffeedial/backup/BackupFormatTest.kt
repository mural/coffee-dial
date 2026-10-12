package com.coffeedial.backup

import com.coffeedial.domain.DialInContext
import com.coffeedial.domain.Recipe
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Frozen first released wire format. Keep this fixture unchanged when v2 arrives.
const val V1_FIXTURE = """{
  "format": "coffee-dial-backup",
  "schemaVersion": 1,
  "exportedAt": "2026-10-02T12:00:00Z",
  "beans": [{"id":"bean-1","name":"Café ☕","roaster":"Test"}],
  "shots": [{
    "id":"shot-1","beanId":"bean-1","createdAt":1790942400000,
    "dose":18.5,"output":37.0,"seconds":28.0,"grind":"42",
    "temperature":null,"notes":"Dulce\nChocolate","rating":4
  }]
}"""

class BackupFormatTest {
    @Test
    fun readsFrozenV1AndRoundTripsWithoutLoss() {
        val backup = BackupFormat.decode(V1_FIXTURE)
        assertEquals(BackupFormat.CURRENT_VERSION, backup.schemaVersion)
        assertEquals(null, backup.shots.single().extraWater)
        assertEquals(null, backup.shots.single().style)
        assertEquals("Café ☕", backup.beans.single().name)
        assertEquals("Dulce\nChocolate", backup.shots.single().notes)
        assertEquals(backup, BackupFormat.decode(BackupFormat.encode(backup)))
        val machine = BackupMachineV1("m1", "Robot", "Manual", "")
        val legacy = backup.copy(
            schemaVersion = 8,
            machines = listOf(machine),
            shots = listOf(backup.shots.single().copy(machine = "robot"))
        )
        val linked = BackupFormat.decode(BackupFormat.encode(legacy))
        assertEquals(BackupFormat.CURRENT_VERSION, linked.schemaVersion)
        assertEquals("m1", linked.shots.single().machineId)
        assertEquals("robot", linked.shots.single().machine)
        assertEquals(linked, BackupFormat.decode(BackupFormat.encode(linked)))
        assertFailsWith<BackupException> {
            BackupFormat.encode(
                linked.copy(shots = listOf(linked.shots.single().copy(machineId = "missing")))
            )
        }
        assertFailsWith<BackupException> {
            BackupFormat.encode(linked.copy(schemaVersion = 8))
        }
        assertNull(
            BackupFormat.decode(
                BackupFormat.encode(
                    legacy.copy(
                        machines = listOf(machine, machine.copy(id = "m2", name = "robot"))
                    )
                )
            ).shots.single().machineId
        )
    }

    @Test
    fun roundTripsV2AndRejectsNewDataLabeledAsV1() {
        val old = BackupFormat.decode(V1_FIXTURE)
        val backup = old.copy(
            shots = listOf(old.shots.single().copy(extraWater = 120.5, style = "Americano"))
        )
        assertEquals(backup, BackupFormat.decode(BackupFormat.encode(backup)))
        assertFailsWith<BackupException> { BackupFormat.encode(backup.copy(schemaVersion = 1)) }
        listOf(-1.0, 0.0, 1001.0, Double.NaN).forEach { water ->
            assertFailsWith<BackupException> {
                BackupFormat.encode(
                    backup.copy(shots = listOf(backup.shots.single().copy(extraWater = water)))
                )
            }
        }
    }

    @Test
    fun rejectsFutureVersionBeforeReadingItsPayload() {
        val future = """{"format":"coffee-dial-backup","schemaVersion":999,"differentData":{}}"""
        val error = assertFailsWith<BackupException> { BackupFormat.decode(future) }
        assertTrue(error.message.orEmpty().contains("Actualizá"))
    }

    @Test
    fun rejectsMissingInvalidAndUnsupportedVersions() {
        listOf("0", "-1", "1.5", "\"1\"", "null").forEach { version ->
            assertFailsWith<BackupException> {
                BackupFormat.decode(
                    V1_FIXTURE.replace("\"schemaVersion\": 1", "\"schemaVersion\": $version")
                )
            }
        }
        assertFailsWith<BackupException> { BackupFormat.decode("{}") }
    }

    @Test
    fun rejectsUnknownFieldsInsteadOfSilentlyLosingFutureData() {
        assertFailsWith<BackupException> {
            BackupFormat.decode(
                V1_FIXTURE.replace("\"rating\":4", "\"rating\":4,\"futureField\":true")
            )
        }
    }

    @Test
    fun rejectsCorruptionAndInvalidRelations() {
        listOf(
            V1_FIXTURE.dropLast(4),
            V1_FIXTURE.replace("coffee-dial-backup", "other-app"),
            V1_FIXTURE.replace("\"beanId\":\"bean-1\"", "\"beanId\":\"missing\""),
            V1_FIXTURE.replace("\"dose\":18.5", "\"dose\":0"),
            V1_FIXTURE.replace("\"rating\":4", "\"rating\":7"),
            V1_FIXTURE.replace("2026-10-02T12:00:00Z", "invalid")
        ).forEach { text -> assertFailsWith<BackupException> { BackupFormat.decode(text) } }
    }

    @Test
    fun rejectsDuplicateIdsAndNonFiniteNumbers() {
        val backup = BackupFormat.decode(V1_FIXTURE)
        assertFailsWith<BackupException> {
            BackupFormat.encode(backup.copy(shots = backup.shots + backup.shots))
        }
        assertFailsWith<BackupException> {
            BackupFormat.encode(backup.copy(beans = backup.beans + backup.beans))
        }
        assertFailsWith<BackupException> {
            BackupFormat.encode(
                backup.copy(shots = listOf(backup.shots.single().copy(dose = Double.NaN)))
            )
        }
    }

    @Test
    fun emptyBackupIsValidAndLargeFilesAreRejected() {
        val empty = BackupFormat.create(emptyList(), emptyList())
        assertEquals(empty, BackupFormat.decode(BackupFormat.encode(empty)))
        assertFailsWith<BackupException> {
            BackupFormat.decode(" ".repeat(BackupFormat.MAX_BYTES + 1))
        }
    }

    @Test
    fun legacyDialInGrinderIsCopiedAndLinkedOnlyWhenTheNameIsUnique() {
        val old = BackupFormat.decode(V1_FIXTURE)
        val dialIn = DialInContext(grinder = "Niche Zero")
        val unique = BackupFormat.decode(
            BackupFormat.encode(
                old.copy(
                    schemaVersion = 9,
                    grinders = listOf(BackupGrinderV1("g1", "Niche Zero", "Eléctrico")),
                    shots = listOf(old.shots.single().copy(dialIn = dialIn))
                )
            )
        )
        assertEquals(BackupFormat.CURRENT_VERSION, unique.schemaVersion)
        assertEquals("Niche Zero", unique.shots.single().grinder)
        assertEquals("g1", unique.shots.single().grinderId)
        val ambiguous = BackupFormat.decode(
            BackupFormat.encode(
                old.copy(
                    schemaVersion = 9,
                    grinders = listOf(
                        BackupGrinderV1("g1", "Niche Zero", "Eléctrico"),
                        BackupGrinderV1("g2", "niche zero", "Manual")
                    ),
                    shots = listOf(old.shots.single().copy(dialIn = dialIn))
                )
            )
        )
        assertEquals("Niche Zero", ambiguous.shots.single().grinder)
        assertNull(ambiguous.shots.single().grinderId)
        val unnamed = BackupFormat.decode(
            BackupFormat.encode(
                old.copy(
                    schemaVersion = 9,
                    shots = listOf(old.shots.single().copy(dialIn = dialIn))
                )
            )
        )
        assertEquals("Niche Zero", unnamed.shots.single().grinder)
        assertNull(unnamed.shots.single().grinderId)
        assertTrue(unnamed.grinders.isEmpty())
        assertFailsWith<BackupException> {
            BackupFormat.encode(
                old.copy(
                    schemaVersion = 9,
                    shots = listOf(old.shots.single().copy(grinder = "Niche Zero"))
                )
            )
        }
    }

    @Test
    fun recipeRoundTripsAtSchema11AndIsRejectedOnAnOlderBackup() {
        val old = BackupFormat.decode(V1_FIXTURE)
        val recipe = Recipe(
            id = "r1",
            name = "Diario",
            beanName = "Café ☕",
            roaster = "Test",
            dose = 18.0,
            output = 36.0,
            grind = "42",
            milk = 40.0
        )
        val saved = old.copy(recipes = listOf(recipe))
        assertEquals(saved.recipes, BackupFormat.decode(BackupFormat.encode(saved)).recipes)
        assertFailsWith<BackupException> {
            BackupFormat.encode(old.copy(schemaVersion = 10, recipes = listOf(recipe)))
        }
        assertFailsWith<BackupException> {
            BackupFormat.encode(saved.copy(recipes = listOf(recipe.copy(name = " "))))
        }
    }

    @Test
    fun cupCapacityRoundTripsAtSchema13AndIsRejectedOnAnOlderBackup() {
        val old = BackupFormat.decode(V1_FIXTURE)
        val cup = BackupCupV1("c1", "Taza Loveramics", 180.0, 250.0)
        val saved = old.copy(cups = listOf(cup))
        assertEquals(saved.cups, BackupFormat.decode(BackupFormat.encode(saved)).cups)
        assertFailsWith<BackupException> {
            BackupFormat.encode(old.copy(schemaVersion = 12, cups = listOf(cup)))
        }
        assertFailsWith<BackupException> {
            BackupFormat.encode(saved.copy(cups = listOf(cup.copy(capacity = -10.0))))
        }
    }
}
