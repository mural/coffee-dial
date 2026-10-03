package com.coffeedial.domain

data class Bean(val id: String, val name: String, val roaster: String)

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
    val rating: Int
) {
    val ratio: Double get() = output / dose
}

data class ShotDraft(
    val beanName: String = "Café molido",
    val roaster: String = "",
    val dose: String = "18",
    val output: String = "36",
    val seconds: String = "20",
    val grind: String = "",
    val temperature: String = "",
    val milk: String = "",
    val machine: String = "",
    val notes: String = "",
    val rating: Int = 3
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
        if (rating !in 1..5) put("rating", "Elegí entre 1 y 5")
    }
}

fun String.decimal(): Double? = trim().replace(',', '.').toDoubleOrNull()

private fun String.isNumberIn(range: ClosedFloatingPointRange<Double>): Boolean =
    decimal()?.let { it.isFinite() && it in range } == true
