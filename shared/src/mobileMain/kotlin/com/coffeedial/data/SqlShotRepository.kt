package com.coffeedial.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.db.SqlDriver
import com.coffeedial.backup.BackupBeanV1
import com.coffeedial.backup.BackupCupV1
import com.coffeedial.backup.BackupException
import com.coffeedial.backup.BackupFormat
import com.coffeedial.backup.BackupGrinderV1
import com.coffeedial.backup.BackupMachineV1
import com.coffeedial.backup.BackupShotV1
import com.coffeedial.backup.BackupV1
import com.coffeedial.backup.ImportPlan
import com.coffeedial.backup.ImportSummary
import com.coffeedial.backup.planImport
import com.coffeedial.backup.resolveEquipment
import com.coffeedial.database.CoffeeDatabase
import com.coffeedial.domain.Bean
import com.coffeedial.domain.BeanDraft
import com.coffeedial.domain.BrewDefault
import com.coffeedial.domain.Cup
import com.coffeedial.domain.CupDraft
import com.coffeedial.domain.DialInContext
import com.coffeedial.domain.Grinder
import com.coffeedial.domain.GrinderDraft
import com.coffeedial.domain.Machine
import com.coffeedial.domain.MachineDraft
import com.coffeedial.domain.Recipe
import com.coffeedial.domain.Shot
import com.coffeedial.domain.ShotDraft
import com.coffeedial.domain.TasteProfile
import com.coffeedial.domain.decimal
import com.coffeedial.domain.fromTenPointScale
import com.coffeedial.photos.BeanPhoto
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class SqlShotRepository(driver: SqlDriver) : ShotRepository {
    private val queries = CoffeeDatabase(driver).coffeeQueries

    init {
        // Avoid relying on JSON SQL extensions unavailable on older Android SQLite versions.
        runCatching {
            queries.transaction {
                queries.legacyTasteProfiles().executeAsList().forEach { row ->
                    val taste = row.taste?.let {
                        Json.decodeFromString<TasteProfile>(it).fromTenPointScale()
                    }
                    queries.migrateTasteProfile(taste?.let { Json.encodeToString(it) }, row.id)
                }
            }
        }
    }

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
                cup = it.cup,
                firstDrop = it.first_drop,
                taste = it.taste?.let { value -> Json.decodeFromString<TasteProfile>(value) },
                dialIn = it.dial_in?.let { value -> Json.decodeFromString<DialInContext>(value) },
                machineId = it.machine_id,
                cupId = it.cup_id,
                grinder = it.grinder,
                grinderId = it.grinder_id,
                milkFoam = it.milk_foam?.let(com.coffeedial.domain.MilkFoam::valueOf)
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
        rows.map {
            Bean(
                it.id,
                it.name,
                it.roaster,
                it.photo?.let { value ->
                    Json.decodeFromString<BeanPhoto>(
                        value
                    )
                },
                it.roast_date,
                it.origin,
                it.process,
                it.bag_weight_grams
            )
        }
    }

    override val cups: Flow<List<Cup>> = queries.allCups().asFlow().mapToList(
        Dispatchers.IO
    ).map { rows ->
        rows.map { Cup(it.id, it.name, it.weight, it.capacity) }
    }

    override val brewDefaults: Flow<List<BrewDefault>> = queries.allBrewDefaults().asFlow().mapToList(
        Dispatchers.IO
    ).map { rows ->
        rows.map { BrewDefault(it.id, it.selected_id) }
    }
    override suspend fun setBrewDefault(value: BrewDefault): Unit = withContext(Dispatchers.IO) {
        require(value.isValid())
        queries.setBrewDefault(value.id, value.selectedId)
    }

    override val recipes: Flow<List<Recipe>> = queries.allRecipes().asFlow().mapToList(
        Dispatchers.IO
    ).map { rows ->
        rows.map { Json.decodeFromString<Recipe>(it.payload) }.sortedBy { it.name }
    }

    override val grinders: Flow<List<Grinder>> = queries.allGrinders().asFlow().mapToList(
        Dispatchers.IO
    ).map { rows ->
        rows.map { Grinder(it.id, it.name, it.type) }
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
                "i18n.sync_changed"
            }
            queries.clearShots()
            queries.clearBeans()
            queries.clearMachines()
            queries.clearCups()
            queries.clearGrinders()
            queries.clearRecipes()
            queries.clearBrewDefaults()
            next.beans.forEach {
                queries.insertBean(
                    it.id,
                    it.name,
                    it.roaster,
                    if (it.archived) 1L else 0L,
                    it.photo?.let { photo -> Json.encodeToString(photo) },
                    it.roastDate,
                    it.origin,
                    it.process,
                    it.bagWeightGrams
                )
            }
            next.machines.forEach { queries.insertMachine(it.id, it.name, it.type, it.year) }
            next.cups.forEach { queries.insertCup(it.id, it.name, it.weight, it.capacity) }
            next.grinders.forEach { queries.insertGrinder(it.id, it.name, it.type) }
            next.brewDefaults.forEach { queries.setBrewDefault(it.id, it.selectedId) }
            next.recipes.forEach { queries.insertRecipe(it.id, Json.encodeToString(it)) }
            next.shots.forEach { writeShot(it) }
            queries.writeSyncCheckpoint(checkpoint)
        }
    }

    override suspend fun save(draft: ShotDraft): Unit = withContext(Dispatchers.IO) {
        require(draft.errors().isEmpty()) { "El shot contiene valores inválidos" }
        queries.transaction {
            val name = draft.beanName.trim()
            val roaster = draft.roaster.trim()
            if (queries.findBean(name, roaster).executeAsOneOrNull() == null) {
                queries.insertBean(Uuid.random().toString(), name, roaster, 0L, null, null, null, null, null)
            }
            val bean = queries.findBean(name, roaster).executeAsOne()
            val linked = linkedEquipment(draft)
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
                machine = linked.machine,
                extra_water = draft.extraWater.decimal(),
                style = draft.style.trim().ifBlank { null },
                cup = linked.cup,
                first_drop = draft.firstDrop.decimal(),
                taste = draft.taste?.let { Json.encodeToString(it) },
                dial_in = draft.dialIn?.let { Json.encodeToString(it) },
                machine_id = linked.machineId,
                cup_id = linked.cupId,
                grinder = linked.grinder,
                grinder_id = linked.grinderId,
                milk_foam = draft.milkFoam?.name
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
            check(previous != null) { "i18n.shot_gone" }
            val keepPrevious = previous.name == name && previous.roaster == roaster
            if (!keepPrevious && queries.findBean(name, roaster).executeAsOneOrNull() == null) {
                queries.insertBean(Uuid.random().toString(), name, roaster, 0L, null, null, null, null, null)
            }
            val bean = if (keepPrevious) {
                previous
            } else {
                queries.findBean(name, roaster).executeAsOne()
            }
            val linked = linkedEquipment(draft)
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
                linked.machine,
                draft.extraWater.decimal(),
                draft.style.trim().ifBlank { null },
                linked.cup,
                draft.firstDrop.decimal(),
                draft.taste?.let { Json.encodeToString(it) },
                draft.dialIn?.let { Json.encodeToString(it) },
                linked.machineId,
                linked.cupId,
                linked.grinder,
                linked.grinderId,
                draft.milkFoam?.name,
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
                ) { "i18n.machine_gone" }
                queries.insertMachine(id, draft.name.trim(), draft.type.trim(), draft.year.trim())
                queries.renameShotMachine(draft.name.trim(), id)
            }
        }

    override suspend fun deleteMachine(id: String): Unit = withContext(Dispatchers.IO) {
        queries.transaction {
            queries.clearShotMachine(id)
            queries.deleteMachine(id)
        }
    }

    override suspend fun saveBean(draft: BeanDraft): Unit = withContext(Dispatchers.IO) {
        require(draft.errors().isEmpty()) { "El café contiene valores inválidos" }
        val id = Uuid.random().toString()
        queries.transaction {
            queries.insertBean(
                id,
                draft.name.trim(),
                draft.roaster.trim(),
                0L,
                draft.photo?.let { Json.encodeToString(it) },
                draft.roastDate,
                draft.origin.trim().ifBlank { null },
                draft.process.trim().ifBlank { null },
                draft.bagWeightGrams.decimal()
            )
        }
    }

    override suspend fun updateBean(id: String, draft: BeanDraft): Unit =
        withContext(Dispatchers.IO) {
            require(draft.errors().isEmpty())
            queries.transaction {
                check(
                    queries.allBeans().executeAsList().any {
                        it.id == id && it.archived == 0L
                    }
                ) { "i18n.bean_gone" }
                queries.updateBean(
                    draft.name.trim(),
                    draft.roaster.trim(),
                    draft.roastDate,
                    draft.origin.trim().ifBlank { null },
                    draft.process.trim().ifBlank { null },
                    draft.bagWeightGrams.decimal(),
                    id
                )
                queries.setBeanPhoto(
                    draft.photo?.let { Json.encodeToString(it) },
                    id
                )
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
            draft.weight.decimal(),
            draft.capacity.decimal()
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
                ) { "i18n.cup_gone" }
                queries.updateCup(
                    draft.name.trim(),
                    draft.weight.decimal(),
                    draft.capacity.decimal(),
                    id
                )
                queries.renameShotCup(draft.name.trim(), id)
            }
        }

    override suspend fun saveGrinder(draft: GrinderDraft): Unit = withContext(Dispatchers.IO) {
        require(draft.errors().isEmpty()) { "El molinillo contiene valores inválidos" }
        queries.insertGrinder(Uuid.random().toString(), draft.name.trim(), draft.type.trim())
    }

    override suspend fun updateGrinder(id: String, draft: GrinderDraft): Unit =
        withContext(Dispatchers.IO) {
            require(draft.errors().isEmpty())
            queries.transaction {
                check(queries.allGrinders().executeAsList().any { it.id == id }) {
                    "i18n.grinder_gone"
                }
                queries.insertGrinder(id, draft.name.trim(), draft.type.trim())
                queries.renameShotGrinder(draft.name.trim(), id)
            }
        }

    override suspend fun deleteGrinder(id: String): Unit = withContext(Dispatchers.IO) {
        queries.transaction {
            queries.clearShotGrinder(id)
            queries.deleteGrinder(id)
        }
    }

    override suspend fun deleteCup(id: String): Unit = withContext(Dispatchers.IO) {
        queries.transaction {
            queries.clearShotCup(id)
            queries.deleteCup(id)
        }
    }

    override suspend fun saveRecipe(recipe: Recipe): Unit = withContext(Dispatchers.IO) {
        require(recipe.isValid())
        queries.insertRecipe(recipe.id, Json.encodeToString(recipe))
    }
    override suspend fun deleteRecipe(id: String): Unit = withContext(Dispatchers.IO) {
        queries.deleteRecipe(id)
    }

    private fun snapshot(): BackupV1 = BackupFormat.create(
        brewDefaults = queries.allBrewDefaults().executeAsList().map {
            BrewDefault(it.id, it.selected_id)
        },
        recipes = queries.allRecipes().executeAsList().map {
            Json.decodeFromString<Recipe>(it.payload)
        },
        beans = queries.allBeans().executeAsList().map {
            BackupBeanV1(
                it.id,
                it.name,
                it.roaster,
                it.archived != 0L,
                it.photo?.let { value ->
                    Json.decodeFromString<BeanPhoto>(
                        value
                    )
                },
                it.roast_date,
                it.origin,
                it.process,
                it.bag_weight_grams
            )
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
                cup = it.cup,
                firstDrop = it.first_drop,
                taste = it.taste?.let { value -> Json.decodeFromString<TasteProfile>(value) },
                dialIn = it.dial_in?.let { value -> Json.decodeFromString<DialInContext>(value) },
                machineId = it.machine_id,
                cupId = it.cup_id,
                grinder = it.grinder,
                grinderId = it.grinder_id,
                milkFoam = it.milk_foam?.let(com.coffeedial.domain.MilkFoam::valueOf)
            )
        },
        machines = queries.allMachines().executeAsList().map {
            BackupMachineV1(it.id, it.name, it.type, it.year)
        },
        cups = queries.allCups().executeAsList().map {
            BackupCupV1(it.id, it.name, it.weight, it.capacity)
        },
        grinders = queries.allGrinders().executeAsList().map {
            BackupGrinderV1(it.id, it.name, it.type)
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
                    current.cups != prepared.local.cups ||
                    current.grinders != prepared.local.grinders ||
                    current.recipes != prepared.local.recipes ||
                    current.brewDefaults != prepared.local.brewDefaults
                ) {
                    throw BackupException("i18n.history_changed")
                }
                prepared.plan.beans.forEach {
                    queries.insertBean(
                        it.id,
                        it.name,
                        it.roaster,
                        if (it.archived) 1L else 0L,
                        it.photo?.let { photo -> Json.encodeToString(photo) },
                        it.roastDate,
                        it.origin,
                        it.process,
                        it.bagWeightGrams
                    )
                }
                prepared.plan.machines.forEach {
                    queries.insertMachine(it.id, it.name, it.type, it.year)
                }
                prepared.plan.cups.forEach {
                    queries.insertCup(it.id, it.name, it.weight, it.capacity)
                }
                prepared.plan.grinders.forEach { queries.insertGrinder(it.id, it.name, it.type) }
                prepared.plan.brewDefaults.forEach { queries.setBrewDefault(it.id, it.selectedId) }
                prepared.plan.recipes.forEach {
                    queries.insertRecipe(it.id, Json.encodeToString(it))
                }
                prepared.plan.shots.forEach { writeShot(it) }
                prepared.summary
            }
        }

    override suspend fun forceImportBackup(text: String): ImportSummary =
        withContext(Dispatchers.IO) {
            val incoming = BackupFormat.decode(text)
            queries.transaction {
                incoming.beans.forEach {
                    queries.insertBean(
                        it.id,
                        it.name,
                        it.roaster,
                        if (it.archived) 1L else 0L,
                        it.photo?.let { photo -> Json.encodeToString(photo) },
                        it.roastDate,
                        it.origin,
                        it.process,
                        it.bagWeightGrams
                    )
                }
                incoming.machines.forEach {
                    queries.insertMachine(it.id, it.name, it.type, it.year)
                }
                incoming.cups.forEach {
                    queries.insertCup(it.id, it.name, it.weight, it.capacity)
                }
                incoming.grinders.forEach {
                    queries.insertGrinder(it.id, it.name, it.type)
                }
                val existingDefaults = queries.allBrewDefaults().executeAsList().map {
                    it.id
                }.toSet()
                incoming.brewDefaults.filterNot {
                    it.id in existingDefaults
                }.forEach { queries.setBrewDefault(it.id, it.selectedId) }
                incoming.recipes.forEach { queries.insertRecipe(it.id, Json.encodeToString(it)) }
                incoming.shots.forEach { writeShot(it) }
            }
            ImportSummary(
                incoming.beans.size,
                incoming.shots.size,
                incoming.machines.size,
                0,
                0,
                newCups = incoming.cups.size,
                newGrinders = incoming.grinders.size,
                newRecipes = incoming.recipes.size
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
                queries.clearGrinders()
                queries.clearRecipes()
                queries.clearBrewDefaults()
                incoming.beans.forEach {
                    queries.insertBean(
                        it.id,
                        it.name,
                        it.roaster,
                        if (it.archived) 1L else 0L,
                        it.photo?.let { photo -> Json.encodeToString(photo) },
                        it.roastDate,
                        it.origin,
                        it.process,
                        it.bagWeightGrams
                    )
                }
                incoming.machines.forEach {
                    queries.insertMachine(it.id, it.name, it.type, it.year)
                }
                incoming.cups.forEach {
                    queries.insertCup(it.id, it.name, it.weight, it.capacity)
                }
                incoming.grinders.forEach {
                    queries.insertGrinder(it.id, it.name, it.type)
                }
                val existingDefaults = queries.allBrewDefaults().executeAsList().map {
                    it.id
                }.toSet()
                incoming.brewDefaults.filterNot {
                    it.id in existingDefaults
                }.forEach { queries.setBrewDefault(it.id, it.selectedId) }
                incoming.recipes.forEach { queries.insertRecipe(it.id, Json.encodeToString(it)) }
                incoming.shots.forEach { writeShot(it) }
            }
            ImportSummary(
                incoming.beans.size,
                incoming.shots.size,
                incoming.machines.size,
                0,
                0,
                newCups = incoming.cups.size,
                newGrinders = incoming.grinders.size,
                newRecipes = incoming.recipes.size
            )
        }

    private fun linkedEquipment(draft: ShotDraft) = resolveEquipment(
        draft.machineId,
        draft.machine,
        draft.cupId,
        draft.cup,
        queries.allMachines().executeAsList().map {
            BackupMachineV1(it.id, it.name, it.type, it.year)
        },
        queries.allCups().executeAsList().map {
            BackupCupV1(it.id, it.name, it.weight, it.capacity)
        },
        draft.grinderId,
        draft.grinder,
        queries.allGrinders().executeAsList().map { BackupGrinderV1(it.id, it.name, it.type) }
    )

    private fun writeShot(shot: BackupShotV1) {
        queries.insertShot(
            id = shot.id,
            bean_id = shot.beanId,
            created_at = shot.createdAt,
            dose = shot.dose,
            output = shot.output,
            seconds = shot.seconds,
            grind = shot.grind,
            temperature = shot.temperature,
            notes = shot.notes,
            rating = shot.rating.toLong(),
            milk = shot.milk,
            machine = shot.machine,
            extra_water = shot.extraWater,
            style = shot.style,
            cup = shot.cup,
            first_drop = shot.firstDrop,
            taste = shot.taste?.let { Json.encodeToString(it) },
            dial_in = shot.dialIn?.let { Json.encodeToString(it) },
            machine_id = shot.machineId,
            cup_id = shot.cupId,
            grinder = shot.grinder,
            grinder_id = shot.grinderId,
            milk_foam = shot.milkFoam?.name
        )
    }
}

@Suppress("FunctionName")
fun ShotRepository(driver: SqlDriver): ShotRepository = SqlShotRepository(driver)
