package com.coffeedial.sync

import com.coffeedial.auth.AuthProvider
import com.coffeedial.auth.AuthRepository
import com.coffeedial.auth.AuthState
import com.coffeedial.auth.User
import com.coffeedial.backup.BackupFormat
import com.coffeedial.data.SnapshotRepository
import com.coffeedial.data.SnapshotStore
import com.coffeedial.domain.ShotDraft
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class SyncEngineTest {
    private class Auth : AuthRepository {
        override val state = MutableStateFlow<AuthState>(
            AuthState.LoggedIn(User("subject", provider = AuthProvider.GOOGLE))
        )
        var refreshes = 0
        var invalidations = 0
        override suspend fun syncCredential() = "test-credential"
        override suspend fun refreshSyncCredential(rejected: String): String {
            refreshes++
            return "renewed-credential"
        }
        override fun authenticationRequired(rejected: String?) {
            invalidations++
        }
        override suspend fun signInWithGoogle() {}
        override suspend fun signInWithApple() {}
        override suspend fun signOut() {
            state.value = AuthState.LoggedOut
        }
    }
    private class Store : SnapshotStore {
        var value: String? = null
        override suspend fun read() = value
        override suspend fun compareAndSet(expected: String?, next: String): Boolean {
            if (value != expected) return false
            value = next
            return true
        }
    }

    @Test fun photosTravelSeparatelyAndFailedPhotoUploadRemainsPending() = runTest {
        val repo = SnapshotRepository.open(Store())
        val photo = com.coffeedial.photos.referencePhoto
        repo.saveBean(com.coffeedial.domain.BeanDraft("Brasil", "", photo))
        var remote = SyncDocument(backup = BackupFormat.create(emptyList(), emptyList()))
        var uploaded = false
        var fail = true
        val get: suspend (String, Map<String, String>) -> String = { url, _ ->
            if (url.contains(
                    "/api/photos/"
                )
            ) {
                Json.encodeToString(photo)
            } else {
                Json.encodeToString(remote)
            }
        }
        val post: suspend (String, String, Map<String, String>) -> String = { url, body, _ ->
            if (url.contains("/api/photos/")) {
                check(!fail)
                assertEquals(photo, Json.decodeFromString<com.coffeedial.photos.BeanPhoto>(body))
                uploaded = true
                "{}"
            } else {
                assertTrue(uploaded)
                val input = Json.decodeFromString<SyncUpload>(body)
                assertNull(input.backup.beans.single().photo?.jpeg)
                remote = SyncDocument(revision = remote.revision + 1, backup = input.backup)
                Json.encodeToString(remote)
            }
        }
        val engine = SyncEngine(repo, Auth(), get = get, post = post)
        assertTrue(engine.performSync().isFailure)
        assertNull(repo.readSyncLocal().checkpoint)
        assertEquals(photo, repo.beans.value.single().photo)
        fail = false
        assertTrue(engine.performSync().isSuccess)
        val second = SnapshotRepository.open(Store())
        assertTrue(SyncEngine(second, Auth(), get = get, post = post).performSync().isSuccess)
        assertEquals(photo, second.beans.value.single().photo)
    }

    @Test fun failedUploadDoesNotAcknowledgeOrChangeLocalData() = runTest {
        val repo = SnapshotRepository.open(Store())
        repo.save(ShotDraft(grind = "12"))
        val before = repo.readSyncLocal()
        val empty = SyncDocument(backup = BackupFormat.create(emptyList(), emptyList()))
        val engine = SyncEngine(repo, Auth(), get = { _, headers ->
            assertEquals("Bearer test-credential", headers["Authorization"])
            assertNull(headers["X-User-Email"])
            Json.encodeToString(empty)
        }, post = { _, _, _ -> error("network unavailable") })
        assertTrue(engine.performSync().isFailure)
        assertTrue(engine.state.value is SyncState.Error)
        assertTrue(sameData(before.backup, repo.readSyncLocal().backup))
        assertNull(repo.readSyncLocal().checkpoint)
    }

    @Test fun successfulSyncSurvivesReopenAndPropagatesDeletion() = runTest {
        val store = Store()
        var repo = SnapshotRepository.open(store)
        repo.save(ShotDraft(grind = "12"))
        var remote = SyncDocument(backup = BackupFormat.create(emptyList(), emptyList()))
        val get: suspend (String, Map<String, String>) -> String = { _, _ ->
            Json.encodeToString(remote)
        }
        val post: suspend (String, String, Map<String, String>) -> String = { _, body, _ ->
            val wire = Json.parseToJsonElement(body).jsonObject
            assertTrue("protocol" in wire)
            assertTrue("machines" in wire.getValue("backup").jsonObject)
            assertTrue("cups" in wire.getValue("backup").jsonObject)
            val upload = Json.decodeFromString<SyncUpload>(body)
            assertEquals(remote.revision, upload.baseRevision)
            remote = SyncDocument(revision = remote.revision + 1, backup = upload.backup)
            Json.encodeToString(remote)
        }
        assertTrue(SyncEngine(repo, Auth(), get = get, post = post).performSync().isSuccess)
        repo = SnapshotRepository.open(store)
        repo.delete(repo.readSyncLocal().backup.shots.single().id)
        assertTrue(SyncEngine(repo, Auth(), get = get, post = post).performSync().isSuccess)
        assertTrue(remote.backup.shots.isEmpty())
        assertEquals(2, remote.revision)
    }

    @Test fun editsDuringUploadRemainPendingAndAccountSwitchCannotUpload() = runTest {
        val repo = SnapshotRepository.open(Store())
        repo.save(ShotDraft(grind = "12"))
        val auth = Auth()
        val remote = SyncDocument(backup = BackupFormat.create(emptyList(), emptyList()))
        val engine = SyncEngine(
            repo,
            auth,
            get = { _, _ -> Json.encodeToString(remote) },
            post = { _, body, _ ->
                val wire = Json.parseToJsonElement(body).jsonObject
                assertTrue("protocol" in wire)
                assertTrue("machines" in wire.getValue("backup").jsonObject)
                assertTrue("cups" in wire.getValue("backup").jsonObject)
                val upload = Json.decodeFromString<SyncUpload>(body)
                repo.update(upload.backup.shots.single().id, ShotDraft(grind = "14"))
                Json.encodeToString(SyncDocument(revision = 1, backup = upload.backup))
            }
        )
        assertTrue(engine.performSync().isFailure)
        assertEquals("14", repo.readSyncLocal().backup.shots.single().grind)
        assertNull(repo.readSyncLocal().checkpoint)
        val local = repo.readSyncLocal()
        repo.commitSync(
            local,
            local.backup,
            Json.encodeToString(
                SyncCheckpoint("GOOGLE:other", SyncDocument(backup = local.backup))
            )
        )
        val guarded = SyncEngine(repo, auth, get = { _, _ -> error("must not send") })
        assertTrue(guarded.performSync().isFailure)
        assertTrue((guarded.state.value as SyncState.Error).message.contains("otra cuenta"))
    }

    @Test fun unauthorizedRenewsOnceAndRetries() = runTest {
        val repo = SnapshotRepository.open(Store())
        val auth = Auth()
        var calls = 0
        val remote = SyncDocument(backup = BackupFormat.create(emptyList(), emptyList()))
        val engine = SyncEngine(repo, auth, get = { _, headers ->
            calls++
            if (calls == 1) throw SyncHttpException(401, "unauthorized")
            assertEquals("Bearer renewed-credential", headers["Authorization"])
            Json.encodeToString(remote)
        })
        assertTrue(engine.performSync().isSuccess)
        assertEquals(2, calls)
        assertEquals(1, auth.refreshes)
        assertEquals(0, auth.invalidations)
    }

    @Test fun unavailableServiceDoesNotInvalidateSessionOrOverwrite() = runTest {
        val repo = SnapshotRepository.open(Store())
        repo.save(ShotDraft(grind = "12"))
        val before = repo.readSyncLocal()
        val auth = Auth()
        var posts = 0
        val engine = SyncEngine(
            repo,
            auth,
            get = { _, _ -> throw SyncHttpException(503, "service_unavailable") },
            post = { _, _, _ ->
                posts++
                error("must not upload")
            }
        )
        assertTrue(engine.forceUploadToCloud().isFailure)
        assertEquals(0, posts)
        assertEquals(0, auth.refreshes)
        assertEquals(0, auth.invalidations)
        assertEquals(before, repo.readSyncLocal())
    }

    @Test fun rejectedRenewalDoesNotLoop() = runTest {
        val auth = Auth()
        var calls = 0
        val engine = SyncEngine(
            SnapshotRepository.open(Store()),
            auth,
            get = { _, _ ->
                calls++
                throw SyncHttpException(401, "unauthorized")
            }
        )
        assertTrue(engine.performSync().isFailure)
        assertEquals(2, calls)
        assertEquals(1, auth.refreshes)
        assertTrue(auth.invalidations > 0)
    }

    @Test fun restoreDoesNotLoseEditsMadeDuringDownload() = runTest {
        val repo = SnapshotRepository.open(Store())
        val remote = SyncDocument(backup = BackupFormat.create(emptyList(), emptyList()))
        val engine = SyncEngine(repo, Auth(), get = { _, _ ->
            repo.save(ShotDraft(grind = "15"))
            Json.encodeToString(remote)
        })
        assertTrue(engine.replaceWithCloud().isFailure)
        assertEquals("15", repo.readSyncLocal().backup.shots.single().grind)
        assertNull(repo.readSyncLocal().checkpoint)
    }
}
