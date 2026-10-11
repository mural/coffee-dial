package com.coffeedial.domain

import com.coffeedial.backup.BackupException
import com.coffeedial.backup.BackupFormat
import com.coffeedial.backup.V1_FIXTURE
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TasteProfileTest {
    @Test fun optionalValuesAreNotInventedForOldShotsAndRoundTrip() {
        val old = BackupFormat.decode(V1_FIXTURE)
        assertNull(old.shots.single().taste)
        val taste = TasteProfile(TasteImpression.BALANCED, 7, 2, 6)
        val backup = old.copy(shots = listOf(old.shots.single().copy(taste = taste)))
        assertEquals(taste, BackupFormat.decode(BackupFormat.encode(backup)).shots.single().taste)
        assertFailsWith<BackupException> { BackupFormat.encode(backup.copy(schemaVersion = 6)) }
        val partial = backup.copy(
            shots = listOf(backup.shots.single().copy(taste = TasteProfile(body = 0)))
        )
        assertNull(BackupFormat.decode(BackupFormat.encode(partial)).shots.single().taste?.acidity)
    }

    @Test fun validatesIntensityInsteadOfUsingRatingRange() {
        assertTrue(TasteProfile(body = 0, acidity = 10).isValid())
        assertTrue(TasteProfile().isEmpty())
        for (value in listOf(-1, 11)) {
            val taste = TasteProfile(bitterness = value)
            assertFalse(taste.isValid())
            assertTrue("taste" in ShotDraft(taste = taste).errors())
            val old = BackupFormat.decode(V1_FIXTURE)
            assertFailsWith<BackupException> {
                BackupFormat.encode(
                    old.copy(shots = listOf(old.shots.single().copy(taste = taste)))
                )
            }
        }
    }
}
