package com.coffeedial.domain

import kotlinx.serialization.Serializable

/** One stable preference key per catalog, with null representing an explicit unset. */
@Serializable
data class BrewDefault(val id: String, val selectedId: String? = null) {
    fun isValid() = id in setOf("beans", "machines", "grinders", "cups") &&
        (selectedId == null || selectedId.isNotBlank() && selectedId.length <= 200)
}

fun List<BrewDefault>.newShot(
    beans: List<Bean>,
    machines: List<Machine>,
    grinders: List<Grinder>,
    cups: List<Cup>
): ShotDraft {
    fun selected(kind: String) = find { it.id == kind }?.selectedId
    val bean = beans.find { it.id == selected("beans") }
    val machine = machines.find { it.id == selected("machines") }
    val grinder = grinders.find { it.id == selected("grinders") }
    val cup = cups.find { it.id == selected("cups") }
    return ShotDraft(
        beanName = bean?.name ?: ShotDraft().beanName,
        roaster = bean?.roaster.orEmpty(),
        machine = machine?.name.orEmpty(),
        machineId = machine?.id,
        grinder = grinder?.name.orEmpty(),
        grinderId = grinder?.id,
        cup = cup?.name.orEmpty(),
        cupId = cup?.id
    )
}
