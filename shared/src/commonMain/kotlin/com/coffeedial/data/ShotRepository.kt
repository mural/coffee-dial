package com.coffeedial.data

import com.coffeedial.backup.BackupV1
import com.coffeedial.backup.ImportPlan
import com.coffeedial.backup.ImportSummary
import com.coffeedial.domain.Machine
import com.coffeedial.domain.MachineDraft
import com.coffeedial.domain.Shot
import com.coffeedial.domain.ShotDraft
import kotlinx.coroutines.flow.Flow

interface ShotRepository {
    val history: Flow<List<Shot>>
    val machines: Flow<List<Machine>>
    suspend fun save(draft: ShotDraft)
    suspend fun update(id: String, draft: ShotDraft)
    suspend fun delete(id: String)
    suspend fun saveMachine(draft: MachineDraft)
    suspend fun deleteMachine(id: String)
    suspend fun exportBackup(): String
    suspend fun prepareImport(text: String): PreparedImport
    suspend fun importBackup(prepared: PreparedImport): ImportSummary
}

class PreparedImport internal constructor(
    internal val local: BackupV1,
    internal val plan: ImportPlan
) {
    val summary: ImportSummary get() = plan.summary
}
