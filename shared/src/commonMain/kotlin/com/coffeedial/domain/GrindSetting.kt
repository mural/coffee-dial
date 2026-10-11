package com.coffeedial.domain

val grindLevels =
    listOf("Muy fino", "Fino", "Medio fino", "Medio", "Medio grueso", "Grueso", "Muy grueso")

/** A notation helper, not a conversion between grinders or grind sizes. */
data class ProGrindSetting(val rotations: Int, val dial: Int, val clicks: Int) {
    fun notation(): String = "$rotations.$dial.$clicks"

    companion object {
        fun parse(value: String): ProGrindSetting? {
            val parts = value.trim().split('.')
            if (parts.size != 3 ||
                parts.any { it.isEmpty() || it.any { c -> c !in '0'..'9' } }
            ) {
                return null
            }
            val numbers = parts.map { it.toIntOrNull() ?: return null }
            if (numbers.any { it !in 0..9999 }) return null
            return ProGrindSetting(numbers[0], numbers[1], numbers[2])
        }
    }
}
