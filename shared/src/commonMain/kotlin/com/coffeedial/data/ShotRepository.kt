package com.coffeedial.data

import com.coffeedial.backup.BackupV1
import com.coffeedial.backup.ImportPlan
import com.coffeedial.backup.ImportSummary
import com.coffeedial.domain.Bean
import com.coffeedial.domain.BeanDraft
import com.coffeedial.domain.Cup
import com.coffeedial.domain.CupDraft
import com.coffeedial.domain.Machine
import com.coffeedial.domain.MachineDraft
import com.coffeedial.domain.Shot
import com.coffeedial.domain.ShotDraft
import kotlinx.coroutines.flow.Flow

interface ShotRepository {
    suspend fun readSyncLocal(): com.coffeedial.sync.SyncLocal
    suspend fun commitSync(
        expected: com.coffeedial.sync.SyncLocal,
        next: BackupV1,
        checkpoint: String
    )

    val history: Flow<List<Shot>>
    val machines: Flow<List<Machine>>
    val beans: Flow<List<Bean>>
    val cups: Flow<List<Cup>>
    suspend fun save(draft: ShotDraft)
    suspend fun update(id: String, draft: ShotDraft)
    suspend fun delete(id: String)
    suspend fun saveMachine(draft: MachineDraft)
    suspend fun updateMachine(id: String, draft: MachineDraft)
    suspend fun deleteMachine(id: String)
    suspend fun saveBean(draft: BeanDraft)
    suspend fun updateBean(id: String, draft: BeanDraft)
    suspend fun deleteBean(id: String)
    suspend fun saveCup(draft: CupDraft)
    suspend fun updateCup(id: String, draft: CupDraft)
    suspend fun deleteCup(id: String)
    suspend fun exportBackup(): String
    suspend fun prepareImport(text: String): PreparedImport
    suspend fun importBackup(prepared: PreparedImport): ImportSummary
    suspend fun forceImportBackup(text: String): ImportSummary
    suspend fun replaceWithBackup(text: String): ImportSummary
}

class PreparedImport internal constructor(
    internal val local: BackupV1,
    internal val plan: ImportPlan
) {
    val summary: ImportSummary get() = plan.summary
}
