package com.coffeedial.sync

import com.coffeedial.auth.AuthRepository
import com.coffeedial.auth.AuthState
import com.coffeedial.auth.User
import com.coffeedial.backup.BackupException
import com.coffeedial.backup.BackupFormat
import com.coffeedial.data.ShotRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

class SyncEngine(
    private val shotRepository: ShotRepository,
    private val authRepository: AuthRepository,
    private val baseUrl: String = "https://auth-coffee.muralooo.win",
    private val get: suspend (String, Map<String, String>) -> String = ::httpGetText,
    private val post: suspend (String, String, Map<String, String>) -> String = ::httpPostJson
) {
    private val mutex = Mutex()

    // The server needs protocol and empty collections even when they equal Kotlin defaults.
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
    private val mutableState = MutableStateFlow<SyncState>(SyncState.Idle)
    val state: StateFlow<SyncState> = mutableState.asStateFlow()
    private enum class Mode { MERGE, UPLOAD, DOWNLOAD }

    suspend fun performSync(): Result<SyncResponse> = run(Mode.MERGE)
    suspend fun forceUploadToCloud(): Result<SyncResponse> = run(Mode.UPLOAD)
    suspend fun replaceWithCloud(): Result<SyncResponse> = run(Mode.DOWNLOAD)
    suspend fun restoreFromCloud(): Result<SyncResponse> = performSync()

    suspend fun resolveConflict(
        remote: SyncDocument,
        conflict: RecordConflict,
        keepLocal: Boolean
    ): Result<SyncResponse> {
        try {
            mutex.withLock {
                val user = (authRepository.state.value as? AuthState.LoggedIn)?.user
                    ?: error("i18n.resolve_sign_in")
                val account = accountKey(user)
                val local = shotRepository.readSyncLocal()
                val saved = checkpoint(local.checkpoint)
                if (saved != null) {
                    check(acceptsAccount(saved.account, user, account)) {
                        "i18n.other_account"
                    }
                }
                val planned = planResolution(
                    local.backup,
                    saved,
                    remote,
                    conflict,
                    keepLocal,
                    account
                )
                BackupFormat.validate(planned.backup)
                shotRepository.commitSync(
                    local,
                    planned.backup,
                    json.encodeToString(planned.checkpoint)
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            val message = error.message ?: "i18n.resolve_failed"
            mutableState.value = SyncState.Error(message)
            return Result.failure(IllegalStateException(message, error))
        }
        return performSync()
    }

    private fun accountKey(user: User): String =
        user.email?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
            ?.let { "email:$it" } ?: "${user.provider}:${user.id}"

    private fun acceptsAccount(saved: String, user: User, account: String): Boolean =
        saved == account || saved == "${user.provider}:${user.id}" ||
            saved == "${user.provider.name.lowercase()}:${user.id}"

    private fun checkUser(user: User) {
        val current = (authRepository.state.value as? AuthState.LoggedIn)?.user
        check(current?.id == user.id && current.provider == user.provider) {
            "i18n.account_changed"
        }
    }

    private fun document(text: String): SyncDocument {
        val result = json.decodeFromString<SyncDocument>(text)
        check(result.protocol == 2 && result.revision >= 0) {
            "i18n.incompatible"
        }
        BackupFormat.validate(result.backup)
        return result.copy(backup = BackupFormat.migrate(result.backup))
    }

    private suspend fun run(mode: Mode): Result<SyncResponse> = mutex.withLock {
        mutableState.value = SyncState.Syncing
        val initialUser = (authRepository.state.value as? AuthState.LoggedIn)?.user
        try {
            val user = initialUser
                ?: error("i18n.sign_in")
            var token = authRepository.syncCredential()
            checkUser(user)
            if (token == null) {
                authRepository.authenticationRequired(null)
                throw SyncHttpException(401, "unauthorized")
            }
            var renewed = false
            suspend fun request(body: String? = null, path: String = "/api/sync"): String {
                suspend fun send(): String {
                    checkUser(user)
                    val headers = mapOf("Authorization" to "Bearer $token")
                    return if (body == null) {
                        get("$baseUrl$path", headers)
                    } else {
                        post("$baseUrl$path", body, headers)
                    }
                }
                try {
                    return send()
                } catch (error: SyncHttpException) {
                    if (error.status != 401) throw error
                    checkUser(user)
                    val replacement = if (!renewed) {
                        renewed = true
                        authRepository.refreshSyncCredential(requireNotNull(token))
                    } else {
                        null
                    }
                    checkUser(user)
                    if (replacement == null || replacement == token) {
                        authRepository.authenticationRequired(token)
                        throw error
                    }
                    token = replacement
                    try {
                        return send()
                    } catch (retry: SyncHttpException) {
                        if (retry.status == 401) authRepository.authenticationRequired(token)
                        throw retry
                    }
                }
            }
            val account = accountKey(user)
            val local = shotRepository.readSyncLocal()
            val saved = checkpoint(local.checkpoint)
            if (mode != Mode.DOWNLOAD) {
                check(saved == null || acceptsAccount(saved.account, user, account)) {
                    "i18n.other_account_long"
                }
            }
            val photoCache = (local.backup.beans + (saved?.document?.backup?.beans ?: emptyList()))
                .mapNotNull {
                    it.photo
                }.filter { it.jpeg != null }.associateBy { it.id }.toMutableMap()
            suspend fun hydrate(doc: SyncDocument): SyncDocument {
                val beans = doc.backup.beans.map { bean ->
                    val photo = bean.photo
                    if (photo == null) {
                        bean
                    } else {
                        val full =
                            photoCache[photo.id]
                                ?: json.decodeFromString<com.coffeedial.photos.BeanPhoto>(
                                    request(path = "/api/photos/${photo.id}")
                                ).also {
                                    require(it.id == photo.id && it.jpeg != null)
                                    it.validate()
                                    photoCache[it.id] =
                                        it
                                }
                        bean.copy(photo = full)
                    }
                }
                return doc.copy(backup = doc.backup.copy(beans = beans))
            }
            val remote = hydrate(document(request()))
            val merged = if (mode == Mode.MERGE) {
                mergeSync(
                    saved?.document?.backup ?: BackupFormat.create(emptyList(), emptyList()),
                    local.backup,
                    remote,
                    saved?.revived.orEmpty().toSet()
                )
            } else {
                null
            }
            if (merged != null && merged.conflicts.isNotEmpty()) {
                mutableState.value = SyncState.Conflict(merged.conflicts, remote)
                return Result.failure(
                    IllegalStateException("i18n.conflict_count:${merged.conflicts.size}")
                )
            }
            val next = when (mode) {
                Mode.MERGE -> merged!!.backup
                Mode.UPLOAD -> local.backup.copy(schemaVersion = BackupFormat.CURRENT_VERSION)
                Mode.DOWNLOAD -> remote.backup
            }
            BackupFormat.validate(next)
            // Photos are uploaded before their references. Failed uploads never acknowledge sync.
            if (mode != Mode.DOWNLOAD) {
                val known = remote.backup.beans.mapNotNull { it.photo?.id }.toSet()
                for (photo in next.beans.mapNotNull { it.photo }.distinctBy { it.id }) {
                    if (photo.id !in known) {
                        check(photo.jpeg != null) {
                            "i18n.missing_photo"
                        }
                        request(json.encodeToString(photo), "/api/photos/${photo.id}")
                    }
                }
            }
            val acknowledged = if (mode == Mode.DOWNLOAD || sameData(next, remote.backup)) {
                remote
            } else {
                hydrate(
                    document(
                        request(
                            json.encodeToString(
                                SyncUpload(
                                    baseRevision = remote.revision,
                                    backup = next.copy(
                                        beans = next.beans.map {
                                            it.copy(photo = it.photo?.copy(jpeg = null))
                                        }
                                    ),
                                    force = mode == Mode.UPLOAD ||
                                        saved?.revived.orEmpty().isNotEmpty()
                                )
                            )
                        )
                    )
                )
            }
            check(sameData(acknowledged.backup, next)) { "i18n.response" }
            checkUser(user)
            // One atomic CAS also protects explicit restore from edits made during the download.
            shotRepository.commitSync(
                local,
                acknowledged.backup,
                json.encodeToString(SyncCheckpoint(account, acknowledged))
            )
            val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
            mutableState.value = SyncState.Success(now, 0, acknowledged.backup.shots.size)
            Result.success(
                SyncResponse(
                    true,
                    now,
                    acknowledged.backup.beans,
                    acknowledged.backup.shots,
                    acknowledged.backup.machines
                )
            )
        } catch (cancelled: CancellationException) {
            mutableState.value = SyncState.Idle
            throw cancelled
        } catch (error: Exception) {
            val current = (authRepository.state.value as? AuthState.LoggedIn)?.user
            if (error is SyncHttpException && error.status == 401 && initialUser != null &&
                current?.id == initialUser.id && current.provider == initialUser.provider
            ) {
                authRepository.authenticationRequired(null)
            }
            val message = when (error) {
                is SyncHttpException -> error.message.orEmpty()
                is BackupException -> error.message.orEmpty()
                is IllegalStateException -> error.message ?: "i18n.sync_failed"
                else -> "i18n.offline"
            }
            mutableState.value = SyncState.Error(message)
            Result.failure(IllegalStateException(message, error))
        }
    }
}
