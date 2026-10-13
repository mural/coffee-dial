package com.coffeedial.domain

import kotlinx.serialization.Serializable

/** Texture, not additional milk volume. Null means not recorded. */
@Serializable
enum class MilkFoam(val label: String) {
    NONE("Sin espuma"),
    LOW("Poca"),
    MEDIUM("Media"),
    HIGH("Abundante")
}
