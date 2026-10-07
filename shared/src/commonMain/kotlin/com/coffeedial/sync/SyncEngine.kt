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

    private fun checkUser(user: User) {
        val current = (authRepository.state.value as? AuthState.LoggedIn)?.user
        check(current?.id == user.id && current.provider == user.provider) {
            "La cuenta cambió. Volvé a sincronizar; no se reemplazaron tus datos locales."
        }
    }

    private fun document(text: String): SyncDocument {
        val result = json.decodeFromString<SyncDocument>(text)
        check(result.protocol == 2 && result.revision >= 0) {
            "Actualizá Coffee Dial: el formato de sincronización no es compatible."
        }
        BackupFormat.validate(result.backup)
        return result
    }

    private suspend fun run(mode: Mode): Result<SyncResponse> = mutex.withLock {
        mutableState.value = SyncState.Syncing
        val initialUser = (authRepository.state.value as? AuthState.LoggedIn)?.user
        try {
            val user = initialUser
                ?: error("Entrá a Cuenta para iniciar sesión y sincronizar.")
            var token = authRepository.syncCredential()
            checkUser(user)
            if (token == null) {
                authRepository.authenticationRequired(null)
                throw SyncHttpException(401, "unauthorized")
            }
            var renewed = false
            suspend fun request(body: String? = null): String {
                suspend fun send(): String {
                    checkUser(user)
                    val headers = mapOf("Authorization" to "Bearer $token")
                    return if (body == null) {
                        get("$baseUrl/api/sync", headers)
                    } else {
                        post("$baseUrl/api/sync", body, headers)
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
            val account = user.email?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
                ?.let { "email:$it" } ?: "${user.provider}:${user.id}"
            val local = shotRepository.readSyncLocal()
            val saved = checkpoint(local.checkpoint)
            if (mode != Mode.DOWNLOAD) {
                val providerAccount = "${user.provider}:${user.id}"
                check(
                    saved == null || saved.account == account ||
                        saved.account == providerAccount ||
                        saved.account == "${user.provider.name.lowercase()}:${user.id}"
                ) {
                    "Los datos locales pertenecen a otra cuenta. Volvé a esa cuenta para " +
                        "sincronizar, o exportá un backup antes de cambiar de cuenta."
                }
            }
            val remote = document(request())
            val next = when (mode) {
                Mode.MERGE -> mergeSync(
                    saved?.document?.backup ?: BackupFormat.create(emptyList(), emptyList()),
                    local.backup,
                    remote
                )

                Mode.UPLOAD -> local.backup.copy(schemaVersion = BackupFormat.CURRENT_VERSION)

                Mode.DOWNLOAD -> remote.backup
            }
            BackupFormat.validate(next)
            val acknowledged = if (mode == Mode.DOWNLOAD || sameData(next, remote.backup)) {
                remote
            } else {
                document(
                    request(
                        json.encodeToString(
                            SyncUpload(
                                baseRevision = remote.revision,
                                backup = next,
                                force = mode == Mode.UPLOAD
                            )
                        )
                    )
                )
            }
            check(sameData(acknowledged.backup, next)) { "Respuesta de sync incompatible." }
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
                is IllegalStateException -> error.message ?: "No se pudo sincronizar."
                else -> "No se pudo conectar. Tus datos siguen guardados; probá nuevamente."
            }
            mutableState.value = SyncState.Error(message)
            Result.failure(IllegalStateException(message, error))
        }
    }
}
