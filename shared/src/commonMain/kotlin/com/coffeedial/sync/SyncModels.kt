package com.coffeedial.sync

import com.coffeedial.backup.BackupBeanV1
import com.coffeedial.backup.BackupMachineV1
import com.coffeedial.backup.BackupShotV1
import kotlinx.serialization.Serializable

@Serializable
data class SyncRequest(
    val lastSyncTimestamp: Long = 0L,
    val beans: List<BackupBeanV1> = emptyList(),
    val shots: List<BackupShotV1> = emptyList(),
    val machines: List<BackupMachineV1> = emptyList()
)

@Serializable
data class SyncResponse(
    val success: Boolean,
    val serverTimestamp: Long,
    val beans: List<BackupBeanV1> = emptyList(),
    val shots: List<BackupShotV1> = emptyList(),
    val machines: List<BackupMachineV1> = emptyList(),
    val error: String? = null
)

sealed interface SyncState {
    data object Idle : SyncState
    data object Syncing : SyncState
    data class Success(
        val lastSyncedAt: Long,
        val newItemsCount: Int,
        val shotsCount: Int = 0
    ) : SyncState
    data class Error(val message: String) : SyncState
}
