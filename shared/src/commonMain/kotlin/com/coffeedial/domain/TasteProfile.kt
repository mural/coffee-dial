package com.coffeedial.domain

import kotlinx.serialization.Serializable

@Serializable
enum class TasteImpression { TOO_SOUR, TOO_BITTER, WEAK, BALANCED, TOO_STRONG }

@Serializable
data class TasteProfile(
    val impression: TasteImpression? = null,
    val body: Int? = null,
    val acidity: Int? = null,
    val bitterness: Int? = null
) {
    fun isValid(): Boolean = listOf(body, acidity, bitterness).all { it == null || it in 0..10 }
    fun isEmpty(): Boolean =
        impression == null && body == null && acidity == null && bitterness == null
}
