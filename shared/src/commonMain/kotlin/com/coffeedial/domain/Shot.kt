package com.coffeedial.domain

data class Bean(
    val id: String,
    val name: String,
    val roaster: String,
    val photo: com.coffeedial.photos.BeanPhoto? = null
)

data class BeanDraft(
    val name: String = "",
    val roaster: String = "",
    val photo: com.coffeedial.photos.BeanPhoto? = null
) {
    fun errors(): Map<String, String> = buildMap {
        if (name.isBlank()) put("name", "Ingresá el nombre del café")
        if (runCatching { photo?.validate() }.isFailure) put("photo", "La foto no es válida.")
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

data class Cup(val id: String, val name: String, val weight: Double? = null)

data class CupDraft(val name: String = "", val weight: String = "") {
    fun errors(): Map<String, String> = buildMap {
        if (name.isBlank()) put("name", "Ingresá el nombre de la taza")
        if (weight.isNotBlank() &&
            (weight.decimal() == null || !weight.decimal()!!.isFinite() || weight.decimal()!! <= 0)
        ) {
            put("weight", "Ingresá un peso válido en gramos")
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
    val cup: String? = null
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
    val cup: String = ""
) {
    fun errors(): Map<String, String> = buildMap {
        if (beanName.isBlank()) put("beanName", "Ingresá el nombre del café")
        if (!dose.isNumberIn(0.1..100.0)) put("dose", "Usá entre 0,1 y 100 g")
        if (!output.isNumberIn(0.1..1000.0)) put("output", "Usá entre 0,1 y 1000 g")
        if (!seconds.isNumberIn(0.1..600.0)) put("seconds", "Usá entre 0,1 y 600 s")
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
        if (rating !in 1..5) put("rating", "Elegí entre 1 y 5")
    }
}

fun String.decimal(): Double? = trim().replace(',', '.').toDoubleOrNull()

private fun String.isNumberIn(range: ClosedFloatingPointRange<Double>): Boolean =
    decimal()?.let { it.isFinite() && it in range } == true
