package com.coffeedial.domain

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

    fun roastAgeSummary(nowMs: Long): String? {
        val days = daysSinceRoast(nowMs) ?: return null
        return when {
            days == 0 -> "Tostado hoy"
            days == 1 -> "Tostado ayer"
            days in 2..6 -> "Tostado hace $days días (desgasificando)"
            days in 7..30 -> "Tostado hace $days días (ventana ideal ☕)"
            else -> "Tostado hace $days días"
        }
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
    fun errors(): Map<String, String> = buildMap {
        if (name.isBlank()) put("name", "Ingresá el nombre del café")
        if (runCatching { photo?.validate() }.isFailure) put("photo", "La foto no es válida.")
        if (bagWeightGrams.isNotBlank() &&
            (bagWeightGrams.decimal() == null || !bagWeightGrams.decimal()!!.isFinite() || bagWeightGrams.decimal()!! <= 0)
        ) {
            put("bagWeightGrams", "Ingresá un peso de bolsa válido en gramos")
        }
    }
}

data class Machine(val id: String, val name: String, val type: String, val year: String)

data class MachineDraft(
    val name: String = "",
    val type: String = "Espresso",
    val year: String = ""
) {
    fun errors(): Map<String, String> = buildMap {
        if (name.isBlank()) put("name", "Ingresá el nombre de la máquina")
        if (year.isNotBlank() && (year.toIntOrNull() == null || year.toInt() !in 1900..2100)) {
            put("year", "Ingresá un año válido (ej. 2022)")
        }
    }
}

data class Grinder(val id: String, val name: String, val type: String)

data class GrinderDraft(val name: String = "", val type: String = "Eléctrico") {
    fun errors(): Map<String, String> = buildMap {
        if (name.isBlank()) put("name", "Ingresá el nombre del molinillo")
        if (type.isBlank()) put("type", "Elegí el tipo de molinillo")
    }
}

data class Cup(
    val id: String,
    val name: String,
    val weight: Double? = null,
    val capacity: Double? = null
)

data class CupDraft(val name: String = "", val weight: String = "", val capacity: String = "") {
    fun errors(): Map<String, String> = buildMap {
        if (name.isBlank()) put("name", "Ingresá el nombre de la taza")
        if (weight.isNotBlank() &&
            (weight.decimal() == null || !weight.decimal()!!.isFinite() || weight.decimal()!! <= 0)
        ) {
            put("weight", "Ingresá un peso válido en gramos")
        }
        if (capacity.isNotBlank() &&
            (
                capacity.decimal() == null || !capacity.decimal()!!.isFinite() ||
                    capacity.decimal()!! <= 0
                )
        ) {
            put("capacity", "Ingresá una capacidad válida en cm³")
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
    val grinderId: String? = null
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
    val grinderId: String? = null
) {
    fun errors(): Map<String, String> = buildMap {
        if (beanName.isBlank()) put("beanName", "Ingresá el nombre del café")
        if (!dose.isNumberIn(0.1..100.0)) put("dose", "Usá entre 0,1 y 100 g")
        if (!output.isNumberIn(0.1..1000.0)) put("output", "Usá entre 0,1 y 1000 g")
        if (!seconds.isNumberIn(0.1..600.0)) put("seconds", "Usá entre 0,1 y 600 s")
        if (firstDrop.isNotBlank() &&
            (
                firstDrop.decimal()?.let {
                    it.isFinite() && it >= 0 &&
                        it <= (seconds.decimal() ?: -1.0)
                } !=
                    true
                )
        ) {
            put("firstDrop", "La primera gota debe estar entre 0 y el tiempo total")
        }
        if (grind.isBlank()) put("grind", "Ingresá el ajuste de molienda")
        if (temperature.isNotBlank() && !temperature.isNumberIn(1.0..100.0)) {
            put("temperature", "Usá entre 1 y 100 °C o dejalo vacío")
        }
        if (milk.isNotBlank() && !milk.isNumberIn(1.0..200.0)) {
            put("milk", "Usá entre 1 y 200 ml o dejalo vacío")
        }
        if (extraWater.isNotBlank() && !extraWater.isNumberIn(1.0..1000.0)) {
            put("extraWater", "Usá entre 1 y 1000 ml o dejalo vacío")
        }
        if (style.length > 100) put("style", "Usá hasta 100 caracteres")
        if (dialIn?.isValid() == false) put("dialIn", "Revisá los datos del ajuste")
        if (taste?.isValid() == false) put("taste", "Usá valores de 1 a 5")
        if (rating !in 1..5) put("rating", "Elegí entre 1 y 5")
    }
}

fun String.decimal(): Double? = trim().replace(',', '.').toDoubleOrNull()

private fun String.isNumberIn(range: ClosedFloatingPointRange<Double>): Boolean =
    decimal()?.let { it.isFinite() && it in range } == true
