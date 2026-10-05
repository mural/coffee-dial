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

class SyncEngineTest {
    private class Auth : AuthRepository {
        override val state = MutableStateFlow<AuthState>(
            AuthState.LoggedIn(User("subject", provider = AuthProvider.GOOGLE))
        )
        override suspend fun syncCredential() = "test-credential"
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
}
