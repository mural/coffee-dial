package com.coffeedial.domain

import com.coffeedial.ui.AppStrings
import kotlinx.serialization.Serializable

/** Texture, not additional milk volume. Null means not recorded. */
@Serializable
enum class MilkFoam(val label: String) {
    NONE("Sin espuma"),
    LOW("Poca"),
    MEDIUM("Media"),
    HIGH("Abundante");

    fun label(strings: AppStrings): String = when (this) {
        NONE -> strings.foamNone
        LOW -> strings.foamLow
        MEDIUM -> strings.foamMedium
        HIGH -> strings.foamHigh
    }
}
