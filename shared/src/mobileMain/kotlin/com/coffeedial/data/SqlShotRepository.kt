package com.coffeedial.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.db.SqlDriver
import com.coffeedial.backup.BackupBeanV1
import com.coffeedial.backup.BackupException
import com.coffeedial.backup.BackupFormat
import com.coffeedial.backup.BackupMachineV1
import com.coffeedial.backup.BackupShotV1
import com.coffeedial.backup.BackupV1
import com.coffeedial.backup.ImportPlan
import com.coffeedial.backup.ImportSummary
import com.coffeedial.backup.planImport
import com.coffeedial.database.CoffeeDatabase
import com.coffeedial.domain.Bean
import com.coffeedial.domain.Machine
import com.coffeedial.domain.MachineDraft
import com.coffeedial.domain.Shot
import com.coffeedial.domain.ShotDraft
import com.coffeedial.domain.decimal
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SqlShotRepository(driver: SqlDriver) : ShotRepository {
    private val queries = CoffeeDatabase(driver).coffeeQueries

    override val history: Flow<List<Shot>> = queries.history().asFlow().mapToList(
        Dispatchers.IO
    ).map { rows ->
        rows.map {
            Shot(
                it.id, Bean(it.bean_id, it.name, it.roaster), it.created_at,
                it.dose, it.output, it.seconds, it.grind,
                it.temperature, it.milk, it.machine, it.notes, it.rating.toInt()
            )
        }
    }

    override val machines: Flow<List<Machine>> = queries.allMachines().asFlow().mapToList(
        Dispatchers.IO
    ).map { rows ->
        rows.map { Machine(it.id, it.name, it.type, it.year) }
    }

    override suspend fun save(draft: ShotDraft): Unit = withContext(Dispatchers.IO) {
        require(draft.errors().isEmpty()) { "El shot contiene valores inválidos" }
        queries.transaction {
            val name = draft.beanName.trim()
            val roaster = draft.roaster.trim()
            queries.insertBean(Uuid.random().toString(), name, roaster)
            val bean = queries.findBean(name, roaster).executeAsOne()
            queries.insertShot(
                Uuid.random().toString(), bean.id, Clock.System.now().toEpochMilliseconds(),
                requireNotNull(draft.dose.decimal()), requireNotNull(draft.output.decimal()),
                requireNotNull(
                    draft.seconds.decimal()
                ),
                draft.grind.trim(), draft.temperature.decimal(),
                draft.notes.trim(), draft.rating.toLong(), draft.milk.decimal(),
                draft.machine.trim().ifBlank { null }
            )
        }
    }

    override suspend fun delete(id: String): Unit = withContext(Dispatchers.IO) {
        queries.deleteShot(id)
    }

    override suspend fun update(id: String, draft: ShotDraft): Unit = withContext(Dispatchers.IO) {
        require(draft.errors().isEmpty()) { "El shot contiene valores inválidos" }
        queries.transaction {
            val name = draft.beanName.trim()
            val roaster = draft.roaster.trim()
            queries.insertBean(Uuid.random().toString(), name, roaster)
            val bean = queries.findBean(name, roaster).executeAsOne()
            queries.updateShot(
                bean.id,
                requireNotNull(draft.dose.decimal()),
                requireNotNull(draft.output.decimal()),
                requireNotNull(draft.seconds.decimal()),
                draft.grind.trim(),
                draft.temperature.decimal(),
                draft.notes.trim(),
                draft.rating.toLong(),
                draft.milk.decimal(),
                draft.machine.trim().ifBlank { null },
                id
            )
        }
    }

    override suspend fun saveMachine(draft: MachineDraft): Unit = withContext(Dispatchers.IO) {
        require(draft.errors().isEmpty()) { "La máquina contiene valores inválidos" }
        queries.insertMachine(
            Uuid.random().toString(),
            draft.name.trim(),
            draft.type.trim(),
            draft.year.trim()
        )
    }

    override suspend fun deleteMachine(id: String): Unit = withContext(Dispatchers.IO) {
        queries.deleteMachine(id)
    }

    private fun snapshot(): BackupV1 = BackupFormat.create(
        queries.allBeans().executeAsList().map { BackupBeanV1(it.id, it.name, it.roaster) },
        queries.allShots().executeAsList().map {
            BackupShotV1(
                it.id, it.bean_id, it.created_at, it.dose, it.output, it.seconds,
                it.grind, it.temperature, it.milk, it.machine, it.notes, it.rating.toInt()
            )
        },
        queries.allMachines().executeAsList().map {
            BackupMachineV1(it.id, it.name, it.type, it.year)
        }
    )

    override suspend fun exportBackup(): String = withContext(Dispatchers.IO) {
        val backup = queries.transactionWithResult { snapshot() }
        BackupFormat.encode(backup)
    }

    override suspend fun prepareImport(text: String): PreparedImport = withContext(Dispatchers.IO) {
        val incoming = BackupFormat.decode(text)
        queries.transactionWithResult {
            val local = snapshot()
            PreparedImport(local, planImport(local, incoming))
        }
    }

    override suspend fun importBackup(prepared: PreparedImport): ImportSummary =
        withContext(Dispatchers.IO) {
            queries.transactionWithResult {
                val current = snapshot()
                if (current.beans != prepared.local.beans ||
                    current.shots != prepared.local.shots ||
                    current.machines != prepared.local.machines
                ) {
                    throw BackupException(
                        "El historial cambió. " +
                            "Volvé a seleccionar el backup para revisarlo."
                    )
                }
                prepared.plan.beans.forEach { queries.insertBean(it.id, it.name, it.roaster) }
                prepared.plan.machines.forEach {
                    queries.insertMachine(it.id, it.name, it.type, it.year)
                }
                prepared.plan.shots.forEach {
                    queries.insertShot(
                        it.id, it.beanId, it.createdAt, it.dose, it.output, it.seconds,
                        it.grind, it.temperature, it.notes, it.rating.toLong(), it.milk, it.machine
                    )
                }
                prepared.summary
            }
        }
}

@Suppress("FunctionName")
fun ShotRepository(driver: SqlDriver): ShotRepository = SqlShotRepository(driver)
