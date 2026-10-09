package com.coffeedial.photos

import com.coffeedial.backup.BackupBeanV1
import com.coffeedial.backup.BackupException
import com.coffeedial.backup.BackupFormat
import com.coffeedial.data.SnapshotRepository
import com.coffeedial.data.SnapshotStore
import com.coffeedial.domain.BeanDraft
import com.coffeedial.domain.ShotDraft
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

val referencePhoto = BeanPhoto(
    "11111111-1111-4111-8111-111111111111",
    "/9j/4AAQSkZJRgABAQAASABIAAD/4QBMRXhpZgAATU0AKgAAAAgAAYdpAAQAAAABAAAAGgAAAAAAA6ABAAMAAAAB" +
        "AAEAAKACAAQAAAABAAAAEKADAAQAAAABAAAAEAAAAAD/7QA4UGhvdG9zaG9wIDMuMAA4QklNBAQAAAAAAAA4QklN" +
        "BCUAAAAAABDUHYzZjwCyBOmACZjs+EJ+/8AAEQgAEAAQAwEiAAIRAQMRAf/EAB8AAAEFAQEBAQEBAAAAAAAAAAAB" +
        "AgMEBQYHCAkKC//EALUQAAIBAwMCBAMFBQQEAAABfQECAwAEEQUSITFBBhNRYQcicRQygZGhCCNCscEVUtHwJDNi" +
        "coIJChYXGBkaJSYnKCkqNDU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6g4SFhoeIiYqSk5SV" +
        "lpeYmZqio6Slpqeoqaqys7S1tre4ubrCw8TFxsfIycrS09TV1tfY2drh4uPk5ebn6Onq8fLz9PX29/j5+v/EAB8B" +
        "AAMBAQEBAQEBAQEAAAAAAAABAgMEBQYHCAkKC//EALURAAIBAgQEAwQHBQQEAAECdwABAgMRBAUhMQYSQVEHYXET" +
        "IjKBCBRCkaGxwQkjM1LwFWJy0QoWJDThJfEXGBkaJicoKSo1Njc4OTpDREVGR0hJSlNUVVZXWFlaY2RlZmdoaWpz" +
        "dHV2d3h5eoKDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uLj5OXm" +
        "5+jp6vLz9PX29/j5+v/bAEMAAgICAgICAwICAwUDAwMFBgUFBQUGCAYGBgYGCAoICAgICAgKCgoKCgoKCgwMDAwM" +
        "DA4ODg4ODw8PDw8PDw8PD//bAEMBAgICBAQEBwQEBxALCQsQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQ" +
        "EBAQEBAQEBAQEBAQEBAQEBAQEP/dAAQAAf/aAAwDAQACEQMRAD8A+f6KKK/n8/pg/9k="
)

class BeanPhotoTest {
    @Test fun validatesBoundedJpegAndRoundTripsBackup() {
        referencePhoto.validate()
        val backup = BackupFormat.create(
            listOf(BackupBeanV1("b", "Brasil", "", photo = referencePhoto)),
            emptyList()
        )
        assertEquals(backup, BackupFormat.decode(BackupFormat.encode(backup)))
        assertFailsWith<IllegalArgumentException> {
            referencePhoto.copy(jpeg = "x".repeat(136537)).validate()
        }
        assertFailsWith<IllegalArgumentException> {
            referencePhoto.copy(id = "../../other").validate()
        }
        assertFailsWith<BackupException> {
            BackupFormat.encode(
                backup.copy(
                    beans = backup.beans.map {
                        it.copy(photo = referencePhoto.copy(jpeg = null))
                    }
                )
            )
        }
    }

    @Test fun photoPersistsEditsRemovalAndReopeningWithoutDeletingShots() = runTest {
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
        repo.saveBean(BeanDraft("Brasil", "", referencePhoto))
        val bean = repo.beans.value.single()
        assertEquals(referencePhoto, SnapshotRepository.open(store).beans.value.single().photo)
        repo.save(ShotDraft(beanName = "Brasil"))
        repo.updateBean(bean.id, BeanDraft("Brasil", "", null))
        assertNull(SnapshotRepository.open(store).beans.value.single().photo)
        repo.updateBean(bean.id, BeanDraft("Brasil", "", referencePhoto))
        repo.deleteBean(bean.id)
        assertEquals(1, repo.history.value.size)
        assertTrue(repo.beans.value.isEmpty())
        assertNull(BackupFormat.decode(repo.exportBackup()).beans.single().photo)
    }
}
