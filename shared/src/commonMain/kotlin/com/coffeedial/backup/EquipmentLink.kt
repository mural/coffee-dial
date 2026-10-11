package com.coffeedial.backup

data class ResolvedEquipment(
    val machineId: String?,
    val machine: String?,
    val cupId: String?,
    val cup: String?
)

fun uniqueEquipmentId(name: String?, items: List<Pair<String, String>>): String? {
    val key = name?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return null
    return items
        .filter { it.second.trim().lowercase() == key }
        .map { it.first }
        .distinct()
        .singleOrNull()
}

/** Fills ids once for backups written before equipment links existed. Names stay as stored. */
fun linkLegacyEquipment(backup: BackupV1): BackupV1 {
    if (backup.schemaVersion >= 9) return backup
    val machines = backup.machines.map { it.id to it.name }
    val cups = backup.cups.map { it.id to it.name }
    return backup.copy(
        shots = backup.shots.map { shot ->
            shot.copy(
                machineId = uniqueEquipmentId(shot.machine, machines),
                cupId = uniqueEquipmentId(shot.cup, cups)
            )
        }
    )
}

fun resolveEquipment(
    machineId: String?,
    machineName: String?,
    cupId: String?,
    cupName: String?,
    machines: List<BackupMachineV1>,
    cups: List<BackupCupV1>
): ResolvedEquipment {
    val machine = resolveOne(machineId, machineName, machines.map { it.id to it.name })
    val cup = resolveOne(cupId, cupName, cups.map { it.id to it.name })
    return ResolvedEquipment(machine.first, machine.second, cup.first, cup.second)
}

private fun resolveOne(
    id: String?,
    name: String?,
    items: List<Pair<String, String>>
): Pair<String?, String?> {
    val typed = name?.trim()?.ifBlank { null }
    val selected = id?.trim()?.takeIf { it.isNotEmpty() }?.let { value ->
        items.find { it.first == value }
    }
    if (selected != null) return selected.first to selected.second
    val matched = uniqueEquipmentId(typed, items)
    val catalogName = items.find { it.first == matched }?.second
    return matched to (catalogName ?: typed)
}
