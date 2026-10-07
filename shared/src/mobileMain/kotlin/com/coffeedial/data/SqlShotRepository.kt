package com.coffeedial.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.db.SqlDriver
import com.coffeedial.backup.BackupBeanV1
import com.coffeedial.backup.BackupCupV1
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
import com.coffeedial.domain.BeanDraft
import com.coffeedial.domain.Cup
import com.coffeedial.domain.CupDraft
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
                id = it.id,
                bean = Bean(it.bean_id, it.name, it.roaster),
                createdAt = it.created_at,
                dose = it.dose,
                output = it.output,
                seconds = it.seconds,
                grind = it.grind,
                temperature = it.temperature,
                milk = it.milk,
                machine = it.machine,
                notes = it.notes,
                rating = it.rating.toInt(),
                extraWater = it.extra_water,
                style = it.style,
                cup = it.cup
            )
        }
    }

    override val machines: Flow<List<Machine>> = queries.allMachines().asFlow().mapToList(
        Dispatchers.IO
    ).map { rows ->
        rows.map { Machine(it.id, it.name, it.type, it.year) }
    }

    override val beans: Flow<List<Bean>> = queries.activeBeans().asFlow().mapToList(
        Dispatchers.IO
    ).map { rows ->
        rows.map { Bean(it.id, it.name, it.roaster) }
    }

    override val cups: Flow<List<Cup>> = queries.allCups().asFlow().mapToList(
        Dispatchers.IO
    ).map { rows ->
        rows.map { Cup(it.id, it.name, it.weight) }
    }

    override suspend fun readSyncLocal(): com.coffeedial.sync.SyncLocal =
        withContext(Dispatchers.IO) {
            queries.transactionWithResult {
                com.coffeedial.sync.SyncLocal(
                    snapshot(),
                    queries.readSyncCheckpoint().executeAsOneOrNull()
                )
            }
        }

    override suspend fun commitSync(
        expected: com.coffeedial.sync.SyncLocal,
        next: BackupV1,
        checkpoint: String
    ): Unit = withContext(Dispatchers.IO) {
        BackupFormat.validate(next)
        queries.transaction {
            check(
                com.coffeedial.sync.sameData(snapshot(), expected.backup) &&
                    queries.readSyncCheckpoint().executeAsOneOrNull() == expected.checkpoint
            ) {
                "Los datos cambiaron durante el sync. Tus cambios están guardados; volvé a sincronizar."
            }
            queries.clearShots()
            queries.clearBeans()
            queries.clearMachines()
            queries.clearCups()
            next.beans.forEach {
                queries.insertBean(it.id, it.name, it.roaster, if (it.archived) 1L else 0L)
            }
            next.machines.forEach { queries.insertMachine(it.id, it.name, it.type, it.year) }
            next.cups.forEach { queries.insertCup(it.id, it.name, it.weight) }
            next.shots.forEach {
                queries.insertShot(
                    id = it.id,
                    bean_id = it.beanId,
                    created_at = it.createdAt,
                    dose = it.dose,
                    output = it.output,
                    seconds = it.seconds,
                    grind = it.grind,
                    temperature = it.temperature,
                    notes = it.notes,
                    rating = it.rating.toLong(),
                    milk = it.milk,
                    machine = it.machine,
                    extra_water = it.extraWater,
                    style = it.style,
                    cup = it.cup
                )
            }
            queries.writeSyncCheckpoint(checkpoint)
        }
    }

    override suspend fun save(draft: ShotDraft): Unit = withContext(Dispatchers.IO) {
        require(draft.errors().isEmpty()) { "El shot contiene valores inválidos" }
        queries.transaction {
            val name = draft.beanName.trim()
            val roaster = draft.roaster.trim()
            if (queries.findBean(name, roaster).executeAsOneOrNull() == null) {
                queries.insertBean(Uuid.random().toString(), name, roaster, 0L)
            }
            val bean = queries.findBean(name, roaster).executeAsOne()
            queries.insertShot(
                id = Uuid.random().toString(),
                bean_id = bean.id,
                created_at = Clock.System.now().toEpochMilliseconds(),
                dose = requireNotNull(draft.dose.decimal()),
                output = requireNotNull(draft.output.decimal()),
                seconds = requireNotNull(draft.seconds.decimal()),
                grind = draft.grind.trim(),
                temperature = draft.temperature.decimal(),
                notes = draft.notes.trim(),
                rating = draft.rating.toLong(),
                milk = draft.milk.decimal(),
                machine = draft.machine.trim().ifBlank { null },
                extra_water = draft.extraWater.decimal(),
                style = draft.style.trim().ifBlank { null },
                cup = draft.cup.trim().ifBlank { null }
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
            val previous = queries.beanForShot(id).executeAsOneOrNull()
            check(previous != null) { "Este shot ya no existe." }
            val keepPrevious = previous.name == name && previous.roaster == roaster
            if (!keepPrevious && queries.findBean(name, roaster).executeAsOneOrNull() == null) {
                queries.insertBean(Uuid.random().toString(), name, roaster, 0L)
            }
            val bean = if (keepPrevious) {
                previous
            } else {
                queries.findBean(name, roaster).executeAsOne()
            }
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
                draft.extraWater.decimal(),
                draft.style.trim().ifBlank { null },
                draft.cup.trim().ifBlank { null },
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

    override suspend fun updateMachine(id: String, draft: MachineDraft): Unit =
        withContext(Dispatchers.IO) {
            require(draft.errors().isEmpty())
            queries.transaction {
                check(
                    queries.allMachines().executeAsList().any {
                        it.id == id
                    }
                ) { "La máquina ya no existe." }
                queries.insertMachine(id, draft.name.trim(), draft.type.trim(), draft.year.trim())
            }
        }

    override suspend fun deleteMachine(id: String): Unit = withContext(Dispatchers.IO) {
        queries.deleteMachine(id)
    }

    override suspend fun saveBean(draft: BeanDraft): Unit = withContext(Dispatchers.IO) {
        require(draft.errors().isEmpty()) { "El café contiene valores inválidos" }
        queries.insertBean(
            Uuid.random().toString(),
            draft.name.trim(),
            draft.roaster.trim(),
            0L
        )
    }

    override suspend fun updateBean(id: String, draft: BeanDraft): Unit =
        withContext(Dispatchers.IO) {
            require(draft.errors().isEmpty())
            queries.transaction {
                check(
                    queries.allBeans().executeAsList().any {
                        it.id == id && it.archived == 0L
                    }
                ) { "El café ya no existe." }
                queries.updateBean(draft.name.trim(), draft.roaster.trim(), id)
            }
        }

    override suspend fun deleteBean(id: String): Unit = withContext(Dispatchers.IO) {
        queries.deleteBean(id)
    }

    override suspend fun saveCup(draft: CupDraft): Unit = withContext(Dispatchers.IO) {
        require(draft.errors().isEmpty()) { "La taza contiene valores inválidos" }
        queries.insertCup(
            Uuid.random().toString(),
            draft.name.trim(),
            draft.weight.decimal()
        )
    }

    override suspend fun updateCup(id: String, draft: CupDraft): Unit =
        withContext(Dispatchers.IO) {
            require(draft.errors().isEmpty())
            queries.transaction {
                check(
                    queries.allCups().executeAsList().any {
                        it.id == id
                    }
                ) { "La taza ya no existe." }
                queries.updateCup(draft.name.trim(), draft.weight.decimal(), id)
            }
        }

    override suspend fun deleteCup(id: String): Unit = withContext(Dispatchers.IO) {
        queries.deleteCup(id)
    }

    private fun snapshot(): BackupV1 = BackupFormat.create(
        beans = queries.allBeans().executeAsList().map {
            BackupBeanV1(it.id, it.name, it.roaster, it.archived != 0L)
        },
        shots = queries.allShots().executeAsList().map {
            BackupShotV1(
                id = it.id,
                beanId = it.bean_id,
                createdAt = it.created_at,
                dose = it.dose,
                output = it.output,
                seconds = it.seconds,
                grind = it.grind,
                temperature = it.temperature,
                milk = it.milk,
                machine = it.machine,
                notes = it.notes,
                rating = it.rating.toInt(),
                extraWater = it.extra_water,
                style = it.style,
                cup = it.cup
            )
        },
        machines = queries.allMachines().executeAsList().map {
            BackupMachineV1(it.id, it.name, it.type, it.year)
        },
        cups = queries.allCups().executeAsList().map {
            BackupCupV1(it.id, it.name, it.weight)
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
                    current.machines != prepared.local.machines ||
                    current.cups != prepared.local.cups
                ) {
                    throw BackupException(
                        "El historial cambió. " +
                            "Volvé a seleccionar el backup para revisarlo."
                    )
                }
                prepared.plan.beans.forEach {
                    queries.insertBean(it.id, it.name, it.roaster, if (it.archived) 1L else 0L)
                }
                prepared.plan.machines.forEach {
                    queries.insertMachine(it.id, it.name, it.type, it.year)
                }
                prepared.plan.cups.forEach { queries.insertCup(it.id, it.name, it.weight) }
                prepared.plan.shots.forEach {
                    queries.insertShot(
                        id = it.id,
                        bean_id = it.beanId,
                        created_at = it.createdAt,
                        dose = it.dose,
                        output = it.output,
                        seconds = it.seconds,
                        grind = it.grind,
                        temperature = it.temperature,
                        notes = it.notes,
                        rating = it.rating.toLong(),
                        milk = it.milk,
                        machine = it.machine,
                        extra_water = it.extraWater,
                        style = it.style,
                        cup = it.cup
                    )
                }
                prepared.summary
            }
        }

    override suspend fun forceImportBackup(text: String): ImportSummary =
        withContext(Dispatchers.IO) {
            val incoming = BackupFormat.decode(text)
            queries.transaction {
                incoming.beans.forEach {
                    queries.insertBean(it.id, it.name, it.roaster, if (it.archived) 1L else 0L)
                }
                incoming.machines.forEach {
                    queries.insertMachine(it.id, it.name, it.type, it.year)
                }
                incoming.cups.forEach {
                    queries.insertCup(it.id, it.name, it.weight)
                }
                incoming.shots.forEach {
                    queries.insertShot(
                        id = it.id,
                        bean_id = it.beanId,
                        created_at = it.createdAt,
                        dose = it.dose,
                        output = it.output,
                        seconds = it.seconds,
                        grind = it.grind,
                        temperature = it.temperature,
                        notes = it.notes,
                        rating = it.rating.toLong(),
                        milk = it.milk,
                        machine = it.machine,
                        extra_water = it.extraWater,
                        style = it.style,
                        cup = it.cup
                    )
                }
            }
            ImportSummary(
                incoming.beans.size,
                incoming.shots.size,
                incoming.machines.size,
                0,
                0,
                newCups = incoming.cups.size
            )
        }

    override suspend fun replaceWithBackup(text: String): ImportSummary =
        withContext(Dispatchers.IO) {
            val incoming = BackupFormat.decode(text)
            queries.transaction {
                queries.clearShots()
                queries.clearBeans()
                queries.clearMachines()
                queries.clearCups()
                incoming.beans.forEach {
                    queries.insertBean(it.id, it.name, it.roaster, if (it.archived) 1L else 0L)
                }
                incoming.machines.forEach {
                    queries.insertMachine(it.id, it.name, it.type, it.year)
                }
                incoming.cups.forEach {
                    queries.insertCup(it.id, it.name, it.weight)
                }
                incoming.shots.forEach {
                    queries.insertShot(
                        id = it.id,
                        bean_id = it.beanId,
                        created_at = it.createdAt,
                        dose = it.dose,
                        output = it.output,
                        seconds = it.seconds,
                        grind = it.grind,
                        temperature = it.temperature,
                        notes = it.notes,
                        rating = it.rating.toLong(),
                        milk = it.milk,
                        machine = it.machine,
                        extra_water = it.extraWater,
                        style = it.style,
                        cup = it.cup
                    )
                }
            }
            ImportSummary(
                incoming.beans.size,
                incoming.shots.size,
                incoming.machines.size,
                0,
                0,
                newCups = incoming.cups.size
            )
        }
}

@Suppress("FunctionName")
fun ShotRepository(driver: SqlDriver): ShotRepository = SqlShotRepository(driver)
