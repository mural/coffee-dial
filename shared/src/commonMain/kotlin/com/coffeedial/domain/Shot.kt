package com.coffeedial.domain

import com.coffeedial.ui.AppStrings
import com.coffeedial.ui.SpanishStrings

data class Bean(
    val id: String,
    val name: String,
    val roaster: String,
    val photo: com.coffeedial.photos.BeanPhoto? = null,
    val roastDate: Long? = null,
    val origin: String? = null,
    val process: String? = null,
    val bagWeightGrams: Double? = null
) {
    fun daysSinceRoast(nowMs: Long): Int? {
        if (roastDate == null || roastDate > nowMs) return null
        return ((nowMs - roastDate) / (24 * 3600 * 1000L)).toInt()
    }

    fun roastAgeSummary(nowMs: Long, strings: AppStrings = SpanishStrings): String? {
        val days = daysSinceRoast(nowMs) ?: return null
        return strings.roastAge(days)
    }
}

data class BeanDraft(
    val name: String = "",
    val roaster: String = "",
    val photo: com.coffeedial.photos.BeanPhoto? = null,
    val roastDate: Long? = null,
    val origin: String = "",
    val process: String = "",
    val bagWeightGrams: String = ""
) {
    fun errors(strings: AppStrings = SpanishStrings): Map<String, String> = buildMap {
        if (name.isBlank()) put("name", strings.errBeanName)
        if (runCatching { photo?.validate() }.isFailure) put("photo", strings.errPhoto)
        if (bagWeightGrams.isNotBlank() &&
            (
                bagWeightGrams.decimal() == null || !bagWeightGrams.decimal()!!.isFinite() ||
                    bagWeightGrams.decimal()!! <= 0
                )
        ) {
            put("bagWeightGrams", strings.errBagWeight)
        }
    }
}

data class Machine(val id: String, val name: String, val type: String, val year: String)

data class MachineDraft(
    val name: String = "",
    val type: String = "Espresso",
    val year: String = ""
) {
    fun errors(strings: AppStrings = SpanishStrings): Map<String, String> = buildMap {
        if (name.isBlank()) put("name", strings.errMachineName)
        if (year.isNotBlank() && (year.toIntOrNull() == null || year.toInt() !in 1900..2100)) {
            put("year", strings.errYear)
        }
    }
}

data class Grinder(val id: String, val name: String, val type: String)

data class GrinderDraft(val name: String = "", val type: String = "Eléctrico") {
    fun errors(strings: AppStrings = SpanishStrings): Map<String, String> = buildMap {
        if (name.isBlank()) put("name", strings.errGrinderName)
        if (type.isBlank()) put("type", strings.errGrinderType)
    }
}

data class Cup(
    val id: String,
    val name: String,
    val weight: Double? = null,
    val capacity: Double? = null
)

data class CupDraft(val name: String = "", val weight: String = "", val capacity: String = "") {
    fun errors(strings: AppStrings = SpanishStrings): Map<String, String> = buildMap {
        if (name.isBlank()) put("name", strings.errCupName)
        if (weight.isNotBlank() &&
            (weight.decimal() == null || !weight.decimal()!!.isFinite() || weight.decimal()!! <= 0)
        ) {
            put("weight", strings.errWeight)
        }
        if (capacity.isNotBlank() &&
            (
                capacity.decimal() == null || !capacity.decimal()!!.isFinite() ||
                    capacity.decimal()!! <= 0
                )
        ) {
            put("capacity", strings.errCapacity)
        }
    }
}

data class Shot(
    val id: String,
    val bean: Bean,
    val createdAt: Long,
    val dose: Double,
    val output: Double,
    val seconds: Double,
    val grind: String,
    val temperature: Double?,
    val milk: Double?,
    val machine: String?,
    val notes: String,
    val rating: Int,
    val extraWater: Double? = null,
    val style: String? = null,
    val cup: String? = null,
    val firstDrop: Double? = null,
    val taste: TasteProfile? = null,
    val dialIn: DialInContext? = null,
    val machineId: String? = null,
    val cupId: String? = null,
    val grinder: String? = null,
    val grinderId: String? = null,
    val milkFoam: MilkFoam? = null
) {
    val ratio: Double get() = output / dose
}

data class ShotDraft(
    val beanName: String = "Café molido",
    val roaster: String = "",
    val dose: String = "18",
    val output: String = "36",
    val seconds: String = "20",
    val grind: String = "Medio",
    val temperature: String = "",
    val milk: String = "",
    val machine: String = "",
    val notes: String = "",
    val rating: Int = 3,
    val extraWater: String = "",
    val style: String = "",
    val cup: String = "",
    val firstDrop: String = "",
    val taste: TasteProfile? = null,
    val dialIn: DialInContext? = null,
    val machineId: String? = null,
    val cupId: String? = null,
    val grinder: String = "",
    val grinderId: String? = null,
    val milkFoam: MilkFoam? = null
) {
    fun errors(strings: AppStrings = SpanishStrings): Map<String, String> = buildMap {
        if (beanName.isBlank()) put("beanName", strings.errBeanName)
        if (!dose.isNumberIn(0.1..100.0)) put("dose", strings.errDose)
        if (!output.isNumberIn(0.1..1000.0)) put("output", strings.errOutput)
        if (!seconds.isNumberIn(0.1..600.0)) put("seconds", strings.errSeconds)
        if (firstDrop.isNotBlank() &&
            (
                firstDrop.decimal()?.let {
                    it.isFinite() && it >= 0 &&
                        it <= (seconds.decimal() ?: -1.0)
                } !=
                    true
                )
        ) {
            put("firstDrop", strings.errFirstDrop)
        }
        if (grind.isBlank()) put("grind", strings.errGrind)
        if (temperature.isNotBlank() && !temperature.isNumberIn(1.0..100.0)) {
            put("temperature", strings.errTemperature)
        }
        if (milk.isNotBlank() && !milk.isNumberIn(1.0..999.0)) {
            put("milk", strings.errMilk)
        }
        if (extraWater.isNotBlank() && !extraWater.isNumberIn(1.0..1000.0)) {
            put("extraWater", strings.errWater)
        }
        if (style.length > 100) put("style", strings.errStyleLength)
        if (dialIn?.isValid() == false) put("dialIn", strings.errDialIn)
        if (taste?.isValid() == false) put("taste", strings.errTaste)
        if (rating !in 1..5) put("rating", strings.errRating)
    }
}

fun String.decimal(): Double? = trim().replace(',', '.').toDoubleOrNull()

private fun String.isNumberIn(range: ClosedFloatingPointRange<Double>): Boolean =
    decimal()?.let { it.isFinite() && it in range } == true
