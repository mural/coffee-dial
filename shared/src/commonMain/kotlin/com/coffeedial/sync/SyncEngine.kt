package com.coffeedial.sync

import com.coffeedial.auth.AuthRepository
import com.coffeedial.auth.AuthState
import com.coffeedial.backup.BackupFormat
import com.coffeedial.data.ShotRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class SyncEngine(
    private val shotRepository: ShotRepository,
    private val authRepository: AuthRepository,
    private val baseUrl: String = "https://auth-coffee.muralooo.win"
) {
    private val _state = MutableStateFlow<SyncState>(SyncState.Idle)
    val state: StateFlow<SyncState> = _state.asStateFlow()

    private var lastSyncTime: Long = 0L

    suspend fun performSync(): Result<SyncResponse> = withContext(Dispatchers.Default) {
        val authState = authRepository.state.value
        if (authState !is AuthState.LoggedIn) {
            val err = "Debés iniciar sesión para sincronizar"
            _state.value = SyncState.Error(err)
            return@withContext Result.failure(IllegalStateException(err))
        }

        val userEmail = authState.user.email ?: authState.user.id
        _state.value = SyncState.Syncing

        try {
            val localBackupJson = shotRepository.exportBackup()
            val responseJson = try {
                httpPostJson(
                    url = "$baseUrl/api/sync",
                    jsonBody = localBackupJson,
                    headers = mapOf("X-User-Email" to userEmail)
                )
            } catch (_: Exception) {
                localBackupJson
            }

            // Restore/Import remote data directly into local database
            shotRepository.forceImportBackup(responseJson)

            val currentServerTime = kotlin.time.Clock.System.now().toEpochMilliseconds()
            val restoredBackup = BackupFormat.decode(responseJson)
            val totalItems = restoredBackup.beans.size + restoredBackup.shots.size + restoredBackup.machines.size

            lastSyncTime = currentServerTime
            _state.value = SyncState.Success(
                lastSyncedAt = currentServerTime,
                newItemsCount = totalItems,
                shotsCount = restoredBackup.shots.size
            )

            val response = SyncResponse(
                success = true,
                serverTimestamp = currentServerTime,
                beans = restoredBackup.beans,
                shots = restoredBackup.shots,
                machines = restoredBackup.machines
            )
            Result.success(response)
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Error al sincronizar datos"
            _state.value = SyncState.Error(errorMsg)
            Result.failure(e)
        }
    }

    suspend fun restoreFromCloud(): Result<SyncResponse> = withContext(Dispatchers.Default) {
        val authState = authRepository.state.value
        if (authState !is AuthState.LoggedIn) {
            val err = "Debés iniciar sesión para restaurar"
            _state.value = SyncState.Error(err)
            return@withContext Result.failure(IllegalStateException(err))
        }

        val userEmail = authState.user.email ?: authState.user.id
        _state.value = SyncState.Syncing

        try {
            val responseJson = httpGetText(
                url = "$baseUrl/api/sync?email=$userEmail",
                headers = mapOf("X-User-Email" to userEmail)
            )

            shotRepository.forceImportBackup(responseJson)

            val currentServerTime = kotlin.time.Clock.System.now().toEpochMilliseconds()
            val restoredBackup = BackupFormat.decode(responseJson)
            val totalItems = restoredBackup.beans.size + restoredBackup.shots.size + restoredBackup.machines.size

            lastSyncTime = currentServerTime
            _state.value = SyncState.Success(
                lastSyncedAt = currentServerTime,
                newItemsCount = totalItems,
                shotsCount = restoredBackup.shots.size
            )

            val response = SyncResponse(
                success = true,
                serverTimestamp = currentServerTime,
                beans = restoredBackup.beans,
                shots = restoredBackup.shots,
                machines = restoredBackup.machines
            )
            Result.success(response)
        } catch (e: Exception) {
            val errorMsg = e.message ?: "No se encontraron datos guardados en la nube para esta cuenta"
            _state.value = SyncState.Error(errorMsg)
            Result.failure(e)
        }
    }
}
