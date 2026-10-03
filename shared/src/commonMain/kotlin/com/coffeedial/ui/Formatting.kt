package com.coffeedial.ui

import kotlin.math.round
import kotlin.time.Instant

internal fun Double.pretty(): String = (round(this * 10) / 10).toString().removeSuffix(".0")

internal fun timestamp(epochMillis: Long): String =
    Instant.fromEpochMilliseconds(epochMillis).toString().take(16).replace('T', ' ') + " UTC"
