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
                        "Volvé a iniciar sesión con Google para autorizar el sync. Apple " +
                            "todavía no tiene sync habilitado."
                    )
            val account = "${user.provider}:${user.id}"
            val local = shotRepository.readSyncLocal()
            val saved = checkpoint(local.checkpoint)
            check(saved == null || saved.account == account) {
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

    // Restore is a non-destructive merge; an empty cloud never clears local data.
    suspend fun restoreFromCloud(): Result<SyncResponse> = performSync()
}
