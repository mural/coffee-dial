package com.coffeedial.backup

/** Add-only policy: exact duplicates are skipped, differing IDs never overwrite local records. */
data class ImportSummary(
    val newBeans: Int,
    val newShots: Int,
    val newMachines: Int = 0,
    val duplicates: Int,
    val conflicts: Int,
    val newCups: Int = 0
)

internal data class ImportPlan(
    val beans: List<BackupBeanV1>,
    val shots: List<BackupShotV1>,
    val machines: List<BackupMachineV1>,
    val summary: ImportSummary,
    val cups: List<BackupCupV1> = emptyList()
)

internal fun planImport(local: BackupV1, incoming: BackupV1): ImportPlan {
    val beansById = local.beans.associateBy { it.id }.toMutableMap()
    val beansByName = local.beans.associateBy {
        Triple(it.name, it.roaster, it.archived)
    }.toMutableMap()
    val mapping = mutableMapOf<String, String>()
    val newBeans = mutableListOf<BackupBeanV1>()
    var conflicts = 0
    incoming.beans.forEach { bean ->
        val sameId = beansById[bean.id]
        val sameName = beansByName[Triple(bean.name, bean.roaster, bean.archived)]
        when {
            sameId != null && sameId != bean -> conflicts++

            sameId != null -> mapping[bean.id] = sameId.id

            sameName != null && sameName.photo == bean.photo -> mapping[bean.id] = sameName.id

            else -> {
                newBeans += bean
                beansById[bean.id] = bean
                beansByName[Triple(bean.name, bean.roaster, bean.archived)] = bean
                mapping[bean.id] = bean.id
            }
        }
    }
    val machinesById = local.machines.associateBy { it.id }.toMutableMap()
    val machinesByName = local.machines.associateBy { it.name }.toMutableMap()
    val newMachines = mutableListOf<BackupMachineV1>()
    incoming.machines.forEach { machine ->
        val sameId = machinesById[machine.id]
        val sameName = machinesByName[machine.name]
        when {
            sameId != null && sameId != machine -> conflicts++

            sameId != null -> {}

            sameName != null -> {}

            else -> {
                newMachines += machine
                machinesById[machine.id] = machine
                machinesByName[machine.name] = machine
            }
        }
    }
    val cupsById = local.cups.associateBy { it.id }
    val newCups = mutableListOf<BackupCupV1>()
    incoming.cups.forEach { cup ->
        val existing = cupsById[cup.id]
        when {
            existing == null -> newCups += cup
            existing != cup -> conflicts++
        }
    }
    val existingShots = local.shots.associateBy { it.id }
    val newShots = mutableListOf<BackupShotV1>()
    var duplicates = 0
    incoming.shots.forEach { shot ->
        val beanId = mapping[shot.beanId]
        if (beanId == null) {
            conflicts++
        } else {
            val mapped = shot.copy(beanId = beanId)
            val existing = existingShots[shot.id]
            when {
                existing == null -> newShots += mapped
                existing == mapped -> duplicates++
                else -> conflicts++
            }
        }
    }
    return ImportPlan(
        newBeans,
        newShots,
        newMachines,
        ImportSummary(
            newBeans.size,
            newShots.size,
            newMachines.size,
            duplicates,
            conflicts,
            newCups.size
        ),
        newCups
    )
}
