package com.coffeedial.domain

import kotlinx.serialization.Serializable

@Serializable enum class RoastLevel { LIGHT, MEDIUM, DARK, UNKNOWN }

@Serializable enum class BrewMethod { ESPRESSO, FILTER, MOKA, OTHER }

@Serializable enum class StrengthGoal { STRONGER, BALANCED, MILDER }

@Serializable enum class TasteBasis { ESPRESSO, FINISHED_DRINK }

@Serializable
data class DialInContext(
    val roast: RoastLevel? = null,
    val method: BrewMethod? = null,
    val grinder: String = "",
    val goal: StrengthGoal? = null,
    val tasteBasis: TasteBasis? = null,
    val previousShotId: String? = null,
    val favorite: Boolean = false
) {
    fun isValid(): Boolean = grinder.length <= 200 &&
        (previousShotId == null || (previousShotId.isNotBlank() && previousShotId.length <= 200))
}
