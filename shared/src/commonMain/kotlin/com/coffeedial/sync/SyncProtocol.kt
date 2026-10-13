package com.coffeedial.sync

import com.coffeedial.backup.BackupBeanV1
import com.coffeedial.backup.BackupCupV1
import com.coffeedial.backup.BackupFormat
import com.coffeedial.backup.BackupGrinderV1
import com.coffeedial.backup.BackupMachineV1
import com.coffeedial.backup.BackupShotV1
import com.coffeedial.backup.BackupV1
import com.coffeedial.domain.BrewDefault
import com.coffeedial.domain.Recipe
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SyncDocument(
    val protocol: Int = 2,
    val revision: Long = 0,
    val backup: BackupV1,
    val deleted: Map<String, List<String>> = emptyMap()
)

@Serializable
data class SyncUpload(
    val protocol: Int = 2,
    val baseRevision: Long,
    val backup: BackupV1,
    val force: Boolean = false
)

@Serializable
data class SyncCheckpoint(
    val account: String,
    val document: SyncDocument,
    val revived: List<String> = emptyList()
)

data class SyncLocal(val backup: BackupV1, val checkpoint: String?)

data class MergeResult(val backup: BackupV1, val conflicts: List<RecordConflict>)

data class PlannedResolution(val backup: BackupV1, val checkpoint: SyncCheckpoint)

data class ConflictField(val label: String, val local: String, val remote: String)

sealed interface RecordConflict {
    val id: String
    val kind: String
}

data class ShotConflict(
    override val id: String,
    val local: BackupShotV1?,
    val remote: BackupShotV1?
) : RecordConflict {
    override val kind: String get() = "shots"
}

data class BeanConflict(
    override val id: String,
    val local: BackupBeanV1?,
    val remote: BackupBeanV1?
) : RecordConflict {
    override val kind: String get() = "beans"
}

data class MachineConflict(
    override val id: String,
    val local: BackupMachineV1?,
    val remote: BackupMachineV1?
) : RecordConflict {
    override val kind: String get() = "machines"
}

data class CupConflict(override val id: String, val local: BackupCupV1?, val remote: BackupCupV1?) :
    RecordConflict {
    override val kind: String get() = "cups"
}

data class GrinderConflict(
    override val id: String,
    val local: BackupGrinderV1?,
    val remote: BackupGrinderV1?
) : RecordConflict {
    override val kind: String get() = "grinders"
}
data class RecipeConflict(override val id: String, val local: Recipe?, val remote: Recipe?) :
    RecordConflict {
    override val kind: String get() = "recipes"
}

data class BrewDefaultConflict(
    override val id: String,
    val local: BrewDefault?,
    val remote: BrewDefault?,
    val localLabel: String = "Sin predeterminado",
    val remoteLabel: String = "Sin predeterminado"
) : RecordConflict {
    override val kind: String get() = "brewDefaults"
}

fun sameData(a: BackupV1, b: BackupV1): Boolean =
    a.beans.associateBy { it.id } == b.beans.associateBy { it.id } &&
        a.shots.associateBy { it.id } == b.shots.associateBy { it.id } &&
        a.machines.associateBy { it.id } == b.machines.associateBy { it.id } &&
        a.cups.associateBy { it.id } == b.cups.associateBy { it.id } &&
        a.grinders.associateBy { it.id } == b.grinders.associateBy { it.id } &&
        a.recipes.associateBy { it.id } == b.recipes.associateBy { it.id } &&
        a.brewDefaults.associateBy { it.id } == b.brewDefaults.associateBy { it.id }

fun conflictKey(conflict: RecordConflict) = "${conflict.kind}:${conflict.id}"

fun conflictTitle(conflict: RecordConflict): String = when (conflict) {
    is ShotConflict -> "Shot"

    is BeanConflict -> conflict.local?.name ?: conflict.remote?.name ?: "Café"

    is MachineConflict -> conflict.local?.name ?: conflict.remote?.name ?: "Máquina"

    is CupConflict -> conflict.local?.name ?: conflict.remote?.name ?: "Taza"

    is BrewDefaultConflict -> when (conflict.id) {
        "beans" -> "Café predeterminado"
        "machines" -> "Máquina predeterminada"
        "grinders" -> "Molinillo predeterminado"
        else -> "Taza predeterminada"
    }

    is RecipeConflict -> conflict.local?.name ?: conflict.remote?.name ?: "Receta"

    is GrinderConflict -> conflict.local?.name ?: conflict.remote?.name ?: "Molinillo"
}

fun conflictFields(conflict: RecordConflict): List<ConflictField> = when (conflict) {
    is ShotConflict -> fields(conflict.local, conflict.remote, shotFields)

    is BeanConflict -> fields(conflict.local, conflict.remote, beanFields)

    is MachineConflict -> fields(conflict.local, conflict.remote, machineFields)

    is CupConflict -> fields(conflict.local, conflict.remote, cupFields)

    is BrewDefaultConflict -> listOf(
        ConflictField("Selección", conflict.localLabel, conflict.remoteLabel)
    )

    is RecipeConflict -> fields(conflict.local, conflict.remote, recipeFields)

    is GrinderConflict -> fields(conflict.local, conflict.remote, grinderFields)
}

/** Filling a missing equipment id is not a concurrent edit. */
fun reconcileShotEquipment(mine: BackupShotV1, theirs: BackupShotV1): BackupShotV1? {
    if (mine.copy(machineId = null, cupId = null, grinderId = null) !=
        theirs.copy(machineId = null, cupId = null, grinderId = null)
    ) {
        return null
    }
    if (mine.machineId != null && theirs.machineId != null && mine.machineId != theirs.machineId) {
        return null
    }
    if (mine.cupId != null && theirs.cupId != null && mine.cupId != theirs.cupId) return null
    if (mine.grinderId != null && theirs.grinderId != null && mine.grinderId != theirs.grinderId) {
        return null
    }
    return mine.copy(
        machineId = mine.machineId ?: theirs.machineId,
        cupId = mine.cupId ?: theirs.cupId,
        grinderId = mine.grinderId ?: theirs.grinderId
    )
}

/**
 * Three-way comparison. A non-empty conflict list means [MergeResult.backup] is the unchanged
 * local copy and must not be uploaded.
 */
fun mergeSync(
    base: BackupV1,
    local: BackupV1,
    remote: SyncDocument,
    revived: Set<String> = emptySet()
): MergeResult {
    require(remote.protocol == 2 && remote.revision >= 0) {
        "Actualizá Coffee Dial para sincronizar."
    }
    val conflicts = mutableListOf<RecordConflict>()
    fun <T> merge(
        kind: String,
        before: List<T>,
        here: List<T>,
        there: List<T>,
        id: (T) -> String,
        conflict: (String, T?, T?) -> RecordConflict,
        reconcile: (T, T) -> T? = { _, _ -> null }
    ): List<T> {
        val previous = before.associateBy(id)
        val mineById = here.associateBy(id)
        val theirsById = there.associateBy(id)
        val deleted = remote.deleted[kind].orEmpty().toSet()
        return (previous.keys + mineById.keys + theirsById.keys).sorted().mapNotNull { key ->
            val old = previous[key]
            val mine = mineById[key]
            val theirs = theirsById[key]
            if (old == null && mine != null && key in deleted) {
                if ("$kind:$key" in revived && theirs == null) return@mapNotNull mine
                conflicts += conflict(key, mine, theirs)
                return@mapNotNull mine
            }
            val reconciled = if (mine != null && theirs != null && mine != theirs) {
                reconcile(mine, theirs)
            } else {
                null
            }
            when {
                reconciled != null -> reconciled

                mine == theirs -> mine

                mine == old -> theirs

                theirs == old -> mine

                else -> {
                    conflicts += conflict(key, mine, theirs)
                    mine
                }
            }
        }
    }
    val merged = BackupFormat.create(
        merge("beans", base.beans, local.beans, remote.backup.beans, { it.id }, ::BeanConflict),
        merge(
            "shots",
            base.shots,
            local.shots,
            remote.backup.shots,
            { it.id },
            ::ShotConflict,
            ::reconcileShotEquipment
        ),
        merge(
            "machines",
            base.machines,
            local.machines,
            remote.backup.machines,
            { it.id },
            ::MachineConflict
        ),
        merge("cups", base.cups, local.cups, remote.backup.cups, { it.id }, ::CupConflict),
        merge(
            "grinders",
            base.grinders,
            local.grinders,
            remote.backup.grinders,
            { it.id },
            ::GrinderConflict
        ),
        merge("recipes", base.recipes, local.recipes, remote.backup.recipes, {
            it.id
        }, ::RecipeConflict),
        merge("brewDefaults", base.brewDefaults, local.brewDefaults, remote.backup.brewDefaults, {
            it.id
        }, { id, mine, theirs ->
            BrewDefaultConflict(
                id,
                mine,
                theirs,
                defaultLabel(mine, local),
                defaultLabel(theirs, remote.backup)
            )
        })
    )
    return if (conflicts.isEmpty()) {
        MergeResult(merged.also(BackupFormat::validate), emptyList())
    } else {
        MergeResult(local, conflicts)
    }
}

fun planResolution(
    local: BackupV1,
    saved: SyncCheckpoint?,
    remote: SyncDocument,
    conflict: RecordConflict,
    keepLocal: Boolean,
    account: String
): PlannedResolution {
    val base = saved?.document?.backup ?: BackupFormat.create(emptyList(), emptyList())
    val chosen = if (keepLocal) {
        localRecord(
            local,
            conflict
        )
    } else {
        remoteRecord(remote.backup, conflict)
    }
    var next = replaceRecord(local, conflict, chosen)
    if (!keepLocal &&
        chosen is BackupShotV1
    ) {
        next = adoptShotReferences(next, remote.backup, chosen)
    }
    val aligned = replaceRecord(base, conflict, remoteRecord(remote.backup, conflict))
    val key = conflictKey(conflict)
    val remoteMissing = remoteRecord(remote.backup, conflict) == null
    val localMissing = localRecord(local, conflict) == null
    val revived = saved?.revived.orEmpty().filterNot { it == key } +
        if (keepLocal && remoteMissing && !localMissing) listOf(key) else emptyList()
    val document = (saved?.document ?: SyncDocument(revision = remote.revision, backup = base))
        .copy(backup = aligned)
    return PlannedResolution(
        next.copy(schemaVersion = BackupFormat.CURRENT_VERSION),
        SyncCheckpoint(saved?.account ?: account, document, revived.distinct().sorted())
    )
}

private fun localRecord(backup: BackupV1, conflict: RecordConflict): Any? = when (conflict) {
    is ShotConflict -> backup.shots.find { it.id == conflict.id }
    is BeanConflict -> backup.beans.find { it.id == conflict.id }
    is MachineConflict -> backup.machines.find { it.id == conflict.id }
    is CupConflict -> backup.cups.find { it.id == conflict.id }
    is BrewDefaultConflict -> backup.brewDefaults.find { it.id == conflict.id }
    is RecipeConflict -> backup.recipes.find { it.id == conflict.id }
    is GrinderConflict -> backup.grinders.find { it.id == conflict.id }
}

private fun remoteRecord(backup: BackupV1, conflict: RecordConflict): Any? = when (conflict) {
    is ShotConflict -> backup.shots.find { it.id == conflict.id }
    is BeanConflict -> backup.beans.find { it.id == conflict.id }
    is MachineConflict -> backup.machines.find { it.id == conflict.id }
    is CupConflict -> backup.cups.find { it.id == conflict.id }
    is BrewDefaultConflict -> backup.brewDefaults.find { it.id == conflict.id }
    is RecipeConflict -> backup.recipes.find { it.id == conflict.id }
    is GrinderConflict -> backup.grinders.find { it.id == conflict.id }
}

private fun replaceRecord(backup: BackupV1, conflict: RecordConflict, record: Any?): BackupV1 =
    when (conflict) {
        is ShotConflict -> backup.copy(
            shots = backup.shots.filterNot { it.id == conflict.id } +
                listOfNotNull(record as BackupShotV1?)
        )

        is BeanConflict -> backup.copy(
            beans = backup.beans.filterNot { it.id == conflict.id } +
                listOfNotNull(record as BackupBeanV1?)
        )

        is MachineConflict -> backup.copy(
            machines = backup.machines.filterNot { it.id == conflict.id } +
                listOfNotNull(record as BackupMachineV1?)
        )

        is CupConflict -> backup.copy(
            cups = backup.cups.filterNot { it.id == conflict.id } +
                listOfNotNull(record as BackupCupV1?)
        )

        is BrewDefaultConflict -> backup.copy(
            brewDefaults =
                backup.brewDefaults.filterNot { it.id == conflict.id } +
                    listOfNotNull(record as BrewDefault?)
        )

        is RecipeConflict -> backup.copy(
            recipes =
                backup.recipes.filterNot { it.id == conflict.id } + listOfNotNull(record as Recipe?)
        )

        is GrinderConflict -> backup.copy(
            grinders = backup.grinders.filterNot { it.id == conflict.id } +
                listOfNotNull(record as BackupGrinderV1?)
        )
    }

private fun adoptShotReferences(local: BackupV1, remote: BackupV1, shot: BackupShotV1): BackupV1 {
    var next = local
    if (next.beans.none { it.id == shot.beanId }) {
        remote.beans.find {
            it.id == shot.beanId
        }?.let { next = next.copy(beans = next.beans + it) }
    }
    val machineId = shot.machineId
    if (machineId != null && next.machines.none { it.id == machineId }) {
        remote.machines.find { it.id == machineId }?.let {
            next = next.copy(machines = next.machines + it)
        }
    }
    val cupId = shot.cupId
    if (cupId != null && next.cups.none { it.id == cupId }) {
        remote.cups.find { it.id == cupId }?.let { next = next.copy(cups = next.cups + it) }
    }
    val grinderId = shot.grinderId
    if (grinderId != null && next.grinders.none { it.id == grinderId }) {
        remote.grinders.find { it.id == grinderId }?.let {
            next = next.copy(grinders = next.grinders + it)
        }
    }
    return next
}

private data class FieldSource<T>(val label: String, val value: (T) -> String?)

private val shotFields = listOf(
    FieldSource<BackupShotV1>("Café") { it.beanId },
    FieldSource("Dosis") { it.dose.toString() },
    FieldSource("Salida") { it.output.toString() },
    FieldSource("Tiempo") { it.seconds.toString() },
    FieldSource("Molienda") { it.grind },
    FieldSource("Temperatura") { it.temperature?.toString() },
    FieldSource("Leche") { it.milk?.toString() },
    FieldSource("Agua extra") { it.extraWater?.toString() },
    FieldSource("Estilo") { it.style },
    FieldSource("Máquina") { it.machine },
    FieldSource("Vínculo de máquina") { it.machineId },
    FieldSource("Taza") { it.cup },
    FieldSource("Vínculo de taza") { it.cupId },
    FieldSource("Molinillo") { it.grinder },
    FieldSource("Vínculo de molinillo") { it.grinderId },
    FieldSource("Primera gota") { it.firstDrop?.toString() },
    FieldSource("Notas") { it.notes },
    FieldSource("Puntuación") { it.rating.toString() },
    FieldSource("Sabor") { it.taste?.let { taste -> taste.toString() } },
    FieldSource("Ajuste") { it.dialIn?.toString() }
)

private val beanFields = listOf(
    FieldSource<BackupBeanV1>("Nombre") { it.name },
    FieldSource("Tostador") { it.roaster },
    FieldSource("Origen") { it.origin },
    FieldSource("Proceso") { it.process },
    FieldSource("Bolsa (g)") { it.bagWeightGrams?.toString() },
    FieldSource("Archivado") { if (it.archived) "sí" else "no" }
)

private val machineFields = listOf(
    FieldSource<BackupMachineV1>("Nombre") { it.name },
    FieldSource("Tipo") { it.type },
    FieldSource("Año") { it.year }
)

private val cupFields = listOf(
    FieldSource<BackupCupV1>("Nombre") { it.name },
    FieldSource("Peso") { it.weight?.toString() },
    FieldSource("Capacidad") { it.capacity?.toString() }
)

private val recipeFields = listOf(
    FieldSource<Recipe>("Nombre") { it.name },
    FieldSource("Parámetros") { it.copy(id = "", name = "").toString() }
)

private val grinderFields = listOf(
    FieldSource<BackupGrinderV1>("Nombre") { it.name },
    FieldSource("Tipo") { it.type }
)

private fun <T> fields(local: T?, remote: T?, sources: List<FieldSource<T>>): List<ConflictField> {
    if (local == null || remote == null) {
        return listOf(
            ConflictField(
                "Estado",
                if (local ==
                    null
                ) {
                    "Eliminado en este dispositivo"
                } else {
                    "Conservado en este dispositivo"
                },
                if (remote ==
                    null
                ) {
                    "Eliminado en el otro dispositivo"
                } else {
                    "Conservado en el otro dispositivo"
                }
            )
        )
    }
    return sources.mapNotNull { source ->
        val left = source.value(local).orEmpty().ifBlank { "—" }
        val right = source.value(remote).orEmpty().ifBlank { "—" }
        if (left == right) null else ConflictField(source.label, left, right)
    }.ifEmpty { listOf(ConflictField("Registro", "Distinto", "Distinto")) }
}

fun checkpoint(text: String?): SyncCheckpoint? =
    text?.let {
        val saved = Json.decodeFromString<SyncCheckpoint>(it)
        saved.copy(document = saved.document.copy(backup = BackupFormat.migrate(saved.document.backup)))
    }

private fun defaultLabel(value: BrewDefault?, backup: BackupV1): String {
    val id = value?.selectedId ?: return "Sin predeterminado"
    return when (value.id) {
        "beans" -> backup.beans.find { it.id == id }?.let { "${it.name} · ${it.roaster}" }
        "machines" -> backup.machines.find { it.id == id }?.name
        "grinders" -> backup.grinders.find { it.id == id }?.name
        "cups" -> backup.cups.find { it.id == id }?.name
        else -> null
    } ?: "Elemento no disponible"
}
