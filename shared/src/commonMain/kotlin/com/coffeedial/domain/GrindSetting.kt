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

/** Explicit prefixes keep the adjustment portable in the existing text field. */
enum class GrindAdjustmentMode(val prefix: String) {
    TURNS(""), CLICKS("Clicks: "), STEPLESS("Stepless: "), MACRO_MICRO("Macro/micro: "), FREE("");

    fun value(grind: String): String = if (prefix.isNotEmpty() && grind.startsWith(prefix)) {
        grind.removePrefix(prefix)
    } else {
        ""
    }

    fun notation(value: String): String = if (value.isBlank()) "" else prefix + value

    companion object {
        fun detect(grind: String): GrindAdjustmentMode = entries.firstOrNull {
            it.prefix.isNotEmpty() && grind.startsWith(it.prefix)
        } ?: if (ProGrindSetting.parse(grind) != null) TURNS else FREE
    }
}
