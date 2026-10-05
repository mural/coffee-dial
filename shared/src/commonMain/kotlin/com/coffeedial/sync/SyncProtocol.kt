package com.coffeedial.sync

import com.coffeedial.backup.BackupFormat
import com.coffeedial.backup.BackupV1
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
data class SyncUpload(val protocol: Int = 2, val baseRevision: Long, val backup: BackupV1)

@Serializable
data class SyncCheckpoint(val account: String, val document: SyncDocument)
data class SyncLocal(val backup: BackupV1, val checkpoint: String?)
fun sameData(a: BackupV1, b: BackupV1): Boolean =
    a.beans.associateBy { it.id } == b.beans.associateBy { it.id } &&
        a.shots.associateBy { it.id } == b.shots.associateBy { it.id } &&
        a.machines.associateBy { it.id } == b.machines.associateBy { it.id }

/** Three-way comparison detects concurrent edits without trusting device clocks. */
fun mergeSync(base: BackupV1, local: BackupV1, remote: SyncDocument): BackupV1 {
    require(remote.protocol == 2 && remote.revision >= 0) {
        "Actualizá Coffee Dial para sincronizar."
    }
    fun <T> merge(
        kind: String,
        before: List<T>,
        here: List<T>,
        there: List<T>,
        id: (T) -> String
    ): List<T> {
        val b = before.associateBy(id)
        val l = here.associateBy(id)
        val r = there.associateBy(id)
        val deleted = remote.deleted[kind].orEmpty().toSet()
        return (b.keys + l.keys + r.keys).sorted().mapNotNull { key ->
            val old = b[key]
            val mine = l[key]
            val theirs = r[key]
            if (old == null && mine != null &&
                key in deleted
            ) {
                error(
                    "Un registro local fue eliminado en otro dispositivo. Conservamos tus " +
                        "datos; revisá el conflicto."
                )
            }
            when {
                mine == theirs -> mine

                mine == old -> theirs

                theirs == old -> mine

                else -> error(
                    "Hay cambios distintos en el mismo registro ($kind, $key). Conservamos " +
                        "ambas versiones; exportá un backup antes de resolver el conflicto."
                )
            }
        }
    }
    return BackupFormat.create(
        merge("beans", base.beans, local.beans, remote.backup.beans) { it.id },
        merge("shots", base.shots, local.shots, remote.backup.shots) { it.id },
        merge("machines", base.machines, local.machines, remote.backup.machines) { it.id }
    ).also(BackupFormat::validate)
}
fun checkpoint(text: String?): SyncCheckpoint? =
    text?.let { Json.decodeFromString<SyncCheckpoint>(it) }
