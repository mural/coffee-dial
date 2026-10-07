package com.coffeedial.sync

import com.coffeedial.auth.AuthRepository
import com.coffeedial.auth.AuthState
import com.coffeedial.backup.BackupFormat
import com.coffeedial.data.ShotRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SyncEngine(
    private val shotRepository: ShotRepository,
    private val authRepository: AuthRepository,
    private val baseUrl: String = "https://auth-coffee.muralooo.win",
    private val get: suspend (String, Map<String, String>) -> String = ::httpGetText,
    private val post: suspend (String, String, Map<String, String>) -> String = ::httpPostJson
) {
    private val mutableState = MutableStateFlow<SyncState>(SyncState.Idle)
    val state: StateFlow<SyncState> = mutableState.asStateFlow()
    private val mutex = Mutex()
    private val json = Json { encodeDefaults = true }

    suspend fun performSync(): Result<SyncResponse> = mutex.withLock {
        mutableState.value = SyncState.Syncing
        try {
            val user =
                (authRepository.state.value as? AuthState.LoggedIn)?.user
                    ?: error("Debés iniciar sesión para sincronizar.")
            val token =
                authRepository.syncCredential()
                    ?: error(
                        "Volvé a iniciar sesión para autorizar el sync."
                    )
            val account = if (!user.email.isNullOrBlank()) "email:${user.email.lowercase().trim()}" else "${user.provider}:${user.id}"
            val local = shotRepository.readSyncLocal()
            val saved = checkpoint(local.checkpoint)
            val sameAccount = saved == null || saved.account == account || saved.account.startsWith("google:") || saved.account.startsWith("apple:")
            check(sameAccount) {
                "Estos datos están vinculados a otra cuenta. Volvé a esa cuenta para " +
                    "sincronizar; no se enviaron datos."
            }
            val headers = mapOf("Authorization" to "Bearer $token")
            val remote = Json.decodeFromString<SyncDocument>(get("$baseUrl/api/sync", headers))
            BackupFormat.validate(remote.backup)
            val base = saved?.document?.backup ?: BackupFormat.create(emptyList(), emptyList())
            val merged = mergeSync(base, local.backup, remote)
            check((authRepository.state.value as? AuthState.LoggedIn)?.user == user) {
                "La cuenta cambió. Volvé a sincronizar."
            }
            val acknowledged = if (sameData(merged, remote.backup)) {
                remote
            } else {
                Json.decodeFromString<SyncDocument>(
                    post(
                        "$baseUrl/api/sync",
                        json.encodeToString(
                            SyncUpload(baseRevision = remote.revision, backup = merged)
                        ),
                        headers
                    )
                )
            }
            check(acknowledged.protocol == 2 && sameData(acknowledged.backup, merged)) {
                "Respuesta de sync incompatible."
            }
            check((authRepository.state.value as? AuthState.LoggedIn)?.user == user) {
                "La cuenta cambió. Volvé a sincronizar."
            }
            shotRepository.commitSync(
                local,
                acknowledged.backup,
                Json.encodeToString(SyncCheckpoint(account, acknowledged))
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
        } catch (error: CancellationException) {
            mutableState.value = SyncState.Idle
            throw error
        } catch (error: Exception) {
            val message = when {
                error.message.orEmpty().contains(
                    "401"
                ) -> "La autorización venció. Cerrá sesión y volvé a entrar con Google."

                error.message.orEmpty().contains(
                    "409"
                ) ->
                    "Otro dispositivo guardó cambios. " +
                        "Volvé a sincronizar; no se perdió ningún dato."

                else ->
                    error.message
                        ?: "No se pudo sincronizar. Tus datos siguen guardados localmente."
            }
            mutableState.value = SyncState.Error(message)
            Result.failure(IllegalStateException(message, error))
        }
    }

    suspend fun forceUploadToCloud(): Result<SyncResponse> = mutex.withLock {
        mutableState.value = SyncState.Syncing
        try {
            val user =
                (authRepository.state.value as? AuthState.LoggedIn)?.user
                    ?: error("Debés iniciar sesión para sincronizar.")
            val token =
                authRepository.syncCredential()
                    ?: error("Volvé a iniciar sesión con Google para autorizar el sync.")
            val account = if (!user.email.isNullOrBlank()) "email:${user.email.lowercase().trim()}" else "${user.provider}:${user.id}"
            val local = shotRepository.readSyncLocal()
            val headers = mapOf("Authorization" to "Bearer $token")

            val remoteText = runCatching { get("$baseUrl/api/sync", headers) }.getOrDefault("{}")
            val remote = runCatching { Json.decodeFromString<SyncDocument>(remoteText) }.getOrNull()
                ?: SyncDocument(protocol = 2, revision = 0, backup = local.backup)

            val uploadBody = json.encodeToString(
                SyncUpload(baseRevision = remote.revision, backup = local.backup, force = true)
            )

            val responseText = post("$baseUrl/api/sync", uploadBody, headers)
            val acknowledged = Json.decodeFromString<SyncDocument>(responseText)

            shotRepository.commitSync(
                local,
                acknowledged.backup,
                Json.encodeToString(SyncCheckpoint(account, acknowledged))
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
        } catch (error: CancellationException) {
            mutableState.value = SyncState.Idle
            throw error
        } catch (error: Exception) {
            val message = error.message ?: "No se pudo sobreescribir la nube."
            mutableState.value = SyncState.Error(message)
            Result.failure(IllegalStateException(message, error))
        }
    }

    suspend fun replaceWithCloud(): Result<SyncResponse> = mutex.withLock {
        mutableState.value = SyncState.Syncing
        try {
            val user =
                (authRepository.state.value as? AuthState.LoggedIn)?.user
                    ?: error("Debés iniciar sesión para importar de la nube.")
            val token = authRepository.syncCredential()
                ?: error("Volvé a iniciar sesión para autorizar el sync.")
            val headers = mapOf("Authorization" to "Bearer $token")

            val responseJson = get("$baseUrl/api/sync", headers)

            val remoteDoc = runCatching {
                Json.decodeFromString<SyncDocument>(responseJson)
            }.getOrNull()
            val backupText = if (remoteDoc != null) {
                json.encodeToString(remoteDoc.backup)
            } else {
                responseJson
            }

            shotRepository.replaceWithBackup(backupText)

            val restoredBackup = BackupFormat.decode(backupText)
            val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
            val totalItems =
                restoredBackup.beans.size + restoredBackup.shots.size +
                    restoredBackup.machines.size +
                    restoredBackup.cups.size

            mutableState.value = SyncState.Success(
                lastSyncedAt = now,
                newItemsCount = totalItems,
                shotsCount = restoredBackup.shots.size
            )

            Result.success(
                SyncResponse(
                    true,
                    now,
                    restoredBackup.beans,
                    restoredBackup.shots,
                    restoredBackup.machines
                )
            )
        } catch (error: CancellationException) {
            mutableState.value = SyncState.Idle
            throw error
        } catch (error: Exception) {
            val message = error.message ?: "No se pudieron importar los datos de la nube."
            mutableState.value = SyncState.Error(message)
            Result.failure(IllegalStateException(message, error))
        }
    }

    suspend fun restoreFromCloud(): Result<SyncResponse> = performSync()
}
