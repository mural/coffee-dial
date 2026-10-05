package com.coffeedial.data

import com.coffeedial.backup.BackupBeanV1
import com.coffeedial.backup.BackupException
import com.coffeedial.backup.BackupFormat
import com.coffeedial.backup.BackupMachineV1
import com.coffeedial.backup.BackupShotV1
import com.coffeedial.backup.BackupV1
import com.coffeedial.backup.ImportSummary
import com.coffeedial.backup.planImport
import com.coffeedial.domain.Bean
import com.coffeedial.domain.Machine
import com.coffeedial.domain.MachineDraft
import com.coffeedial.domain.Shot
import com.coffeedial.domain.ShotDraft
import com.coffeedial.domain.decimal
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Atomic compare-and-swap prevents stale browser tabs from overwriting data. */
interface SnapshotStore {
    suspend fun read(): String?
    suspend fun compareAndSet(expected: String?, next: String): Boolean
}

@kotlinx.serialization.Serializable
private data class StoredSnapshot(
    val storageVersion: Int = 2,
    val backup: BackupV1,
    val checkpoint: String? = null
)

class SnapshotRepository private constructor(
    private val store: SnapshotStore,
    private var raw: String?
) : ShotRepository {
    private val storageJson = kotlinx.serialization.json.Json { encodeDefaults = true }
    private fun decodeStorage(text: String?): StoredSnapshot = try {
        readStorage(text)
    } catch (_: Exception) {
        throw BackupException(
            "Los datos locales tienen un formato incompatible o dañado. No se modificaron."
        )
    }
    private fun readStorage(text: String?): StoredSnapshot {
        if (text ==
            null
        ) {
            return StoredSnapshot(backup = BackupFormat.create(emptyList(), emptyList()))
        }
        val root = kotlinx.serialization.json.Json.parseToJsonElement(
            text
        ) as kotlinx.serialization.json.JsonObject
        if ("storageVersion" !in root) return StoredSnapshot(backup = BackupFormat.decode(text))
        return kotlinx.serialization.json.Json.decodeFromString<StoredSnapshot>(text).also {
            require(it.storageVersion == 2)
            BackupFormat.validate(it.backup)
        }
    }
    private var stored = decodeStorage(raw)
    private var data = stored.backup
    private val mutex = Mutex()
    override val history = MutableStateFlow<List<Shot>>(emptyList())
    override val machines = MutableStateFlow<List<Machine>>(emptyList())

    init {
        publish()
    }

    companion object {
        suspend fun open(store: SnapshotStore) = SnapshotRepository(store, store.read())
    }

    private fun publish() {
        val beans = data.beans.associateBy { it.id }
        history.value =
            data.shots.sortedWith(
                compareByDescending<BackupShotV1> {
                    it.createdAt
                }.thenBy { it.id }
            ).map {
                val bean = beans[it.beanId] ?: BackupBeanV1(it.beanId, "Café molido", "")
                Shot(
                    it.id, Bean(bean.id, bean.name, bean.roaster), it.createdAt, it.dose, it.output,
                    it.seconds, it.grind, it.temperature, it.milk, it.machine, it.notes, it.rating
                )
            }
        machines.value =
            data.machines.sortedBy { it.name }.map { Machine(it.id, it.name, it.type, it.year) }
    }

    private suspend fun refresh() {
        val latest = store.read()
        if (latest != raw) {
            val decoded = decodeStorage(latest)
            raw = latest
            stored = decoded
            data = decoded.backup
            publish()
        }
    }

    private suspend fun change(transform: (BackupV1) -> BackupV1) = mutex.withLock {
        refresh()
        val next = transform(data).copy(exportedAt = Clock.System.now().toString())
        BackupFormat.validate(next)
        val text = storageJson.encodeToString(stored.copy(backup = next))
        if (!store.compareAndSet(raw, text)) {
            refresh()
            throw BackupException(
                "Los datos cambiaron en otra pestaña. " +
                    "Revisalos e intentá otra vez."
            )
        }
        raw = text
        data = next
        stored = stored.copy(backup = next)
        publish()
    }

    override suspend fun readSyncLocal(): com.coffeedial.sync.SyncLocal = mutex.withLock {
        refresh()
        com.coffeedial.sync.SyncLocal(data, stored.checkpoint)
    }
    override suspend fun commitSync(
        expected: com.coffeedial.sync.SyncLocal,
        next: BackupV1,
        checkpoint: String
    ) = mutex.withLock {
        refresh()
        check(
            com.coffeedial.sync.sameData(data, expected.backup) &&
                stored.checkpoint == expected.checkpoint
        ) {
            "Los datos cambiaron durante el sync. Tus cambios están guardados; volvé a sincronizar."
        }
        BackupFormat.validate(next)
        val nextStored = StoredSnapshot(backup = next, checkpoint = checkpoint)
        val text = storageJson.encodeToString(nextStored)
        check(store.compareAndSet(raw, text)) {
            "Otra pestaña cambió los datos. Volvé a sincronizar."
        }
        raw = text
        stored = nextStored
        data = next
        publish()
    }

    private fun withShot(
        local: BackupV1,
        draft: ShotDraft,
        id: String?,
        createdAt: Long
    ): BackupV1 {
        require(draft.errors().isEmpty())
        val bean =
            local.beans.find {
                it.name == draft.beanName.trim() &&
                    it.roaster == draft.roaster.trim()
            }
                ?: BackupBeanV1(
                    Uuid.random().toString(),
                    draft.beanName.trim(),
                    draft.roaster.trim()
                )
        val shot = BackupShotV1(
            id ?: Uuid.random().toString(), bean.id, createdAt,
            requireNotNull(draft.dose.decimal()), requireNotNull(draft.output.decimal()),
            requireNotNull(
                draft.seconds.decimal()
            ),
            draft.grind.trim(), draft.temperature.decimal(),
            draft.milk.decimal(),
            draft.machine.trim().ifBlank {
                null
            },
            draft.notes.trim(), draft.rating
        )
        return local.copy(
            beans = if (bean in local.beans) local.beans else local.beans + bean,
            shots = local.shots.filterNot { it.id == shot.id } + shot
        )
    }

    override suspend fun save(draft: ShotDraft) = change {
        withShot(it, draft, null, Clock.System.now().toEpochMilliseconds())
    }
    override suspend fun update(id: String, draft: ShotDraft) = change {
        val old =
            it.shots.find { shot -> shot.id == id }
                ?: throw BackupException("Este shot ya no existe.")
        withShot(it, draft, id, old.createdAt)
    }
    override suspend fun delete(id: String) = change {
        it.copy(
            shots = it.shots.filterNot { shot ->
                shot.id ==
                    id
            }
        )
    }
    override suspend fun saveMachine(draft: MachineDraft) = change {
        require(draft.errors().isEmpty())
        if (it.machines.any { machine -> machine.name == draft.name.trim() }) {
            it
        } else {
            it.copy(
                machines =
                    it.machines +
                        BackupMachineV1(
                            Uuid.random().toString(),
                            draft.name.trim(),
                            draft.type.trim(),
                            draft.year.trim()
                        )
            )
        }
    }
    override suspend fun updateMachine(id: String, draft: MachineDraft) = change {
        require(draft.errors().isEmpty())
        check(it.machines.any { machine -> machine.id == id }) { "La máquina ya no existe." }
        it.copy(
            machines = it.machines.map { machine ->
                if (machine.id ==
                    id
                ) {
                    BackupMachineV1(id, draft.name.trim(), draft.type.trim(), draft.year.trim())
                } else {
                    machine
                }
            }
        )
    }
    override suspend fun deleteMachine(id: String) = change {
        it.copy(
            machines = it.machines.filterNot { machine ->
                machine.id ==
                    id
            }
        )
    }
    override suspend fun exportBackup(): String = mutex.withLock {
        refresh()
        BackupFormat.encode(data.copy(exportedAt = Clock.System.now().toString()))
    }
    override suspend fun prepareImport(text: String): PreparedImport = mutex.withLock {
        val incoming = BackupFormat.decode(text)
        refresh()
        PreparedImport(data, planImport(data, incoming))
    }
    override suspend fun importBackup(prepared: PreparedImport): ImportSummary {
        change {
            if (it.beans != prepared.local.beans || it.shots != prepared.local.shots ||
                it.machines != prepared.local.machines
            ) {
                throw BackupException(
                    "El historial cambió. Volvé a seleccionar el backup para revisarlo."
                )
            }
            it.copy(
                beans = it.beans + prepared.plan.beans,
                shots = it.shots + prepared.plan.shots,
                machines = it.machines + prepared.plan.machines
            )
        }
        return prepared.summary
    }

    override suspend fun forceImportBackup(text: String): ImportSummary {
        val incoming = BackupFormat.decode(text)
        change {
            it.copy(
                beans = (it.beans + incoming.beans).distinctBy { bean -> bean.id },
                shots = (it.shots + incoming.shots).distinctBy { shot -> shot.id },
                machines = (it.machines + incoming.machines).distinctBy { machine -> machine.id }
            )
        }
        return ImportSummary(incoming.beans.size, incoming.shots.size, incoming.machines.size, 0, 0)
    }

    override suspend fun replaceWithBackup(text: String): ImportSummary {
        val incoming = BackupFormat.decode(text)
        change {
            it.copy(
                beans = incoming.beans,
                shots = incoming.shots,
                machines = incoming.machines
            )
        }
        return ImportSummary(incoming.beans.size, incoming.shots.size, incoming.machines.size, 0, 0)
    }
}
