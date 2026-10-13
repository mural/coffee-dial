package com.coffeedial.domain

import kotlin.math.round
import kotlinx.serialization.Serializable

/** Independent recipe snapshot; catalog names remain usable after deleting equipment or a bean. */
@Serializable
data class Recipe(
    val id: String,
    val name: String,
    val beanName: String,
    val roaster: String,
    val dose: Double,
    val output: Double,
    val grind: String,
    val temperature: Double? = null,
    val milk: Double? = null,
    val extraWater: Double? = null,
    val machine: String? = null,
    val grinder: String? = null,
    val cup: String? = null,
    val style: String? = null,
    val milkFoam: MilkFoam? = null
) {
    fun toDraft() = ShotDraft(
        beanName = beanName, roaster = roaster, dose = dose.toString(), output = output.toString(),
        grind = grind, temperature = temperature?.toString().orEmpty(),
        milk = milk?.toString().orEmpty(), extraWater = extraWater?.toString().orEmpty(),
        machine = machine.orEmpty(), grinder = grinder.orEmpty(), cup = cup.orEmpty(),
        milkFoam = milkFoam, style = style.orEmpty(), seconds = "", firstDrop = "", notes = "", taste = null, dialIn = null
    )
    fun isValid(): Boolean = id.isNotBlank() && id.length <= 200 &&
        name.isNotBlank() && name.length <= 100 &&
        listOf(beanName, roaster, grind, machine.orEmpty(), grinder.orEmpty(), cup.orEmpty()).all {
            it.length <=
                10000
        } &&
        toDraft().copy(seconds = "1").errors().isEmpty()
}

fun Shot.asRecipe(id: String, name: String) = Recipe(
    id, name.trim(), bean.name, bean.roaster, dose, output, grind, temperature,
    milk, extraWater, machine, grinder ?: dialIn?.grinder, cup, style, milkFoam
)

enum class RatioEdit { DOSE, RATIO, OUTPUT }

data class RecipeAmounts(val dose: Double, val output: Double) {
    val ratio: Double get() = output / dose
    fun change(field: RatioEdit, value: Double): RecipeAmounts? {
        if (!value.isFinite() || value <= 0) return null
        val next = when (field) {
            RatioEdit.DOSE -> RecipeAmounts(value, round(value * ratio * 100) / 100)
            RatioEdit.RATIO -> copy(output = round(dose * value * 100) / 100)
            RatioEdit.OUTPUT -> copy(output = value)
        }
        return next.takeIf { it.dose in 0.1..100.0 && it.output in 0.1..1000.0 }
    }
}

/** Repeat preparation parameters, never the previous extraction's evaluation or experiment. */
fun Shot.repeatDraft(): ShotDraft = asRecipe("repeat", "repeat").toDraft().copy(
    machineId = machineId,
    cupId = cupId,
    grinderId = grinderId
)
