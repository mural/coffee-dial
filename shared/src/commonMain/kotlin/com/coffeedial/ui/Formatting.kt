package com.coffeedial.ui

import kotlin.math.round
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toLocalDateTime

internal fun Double.pretty(): String = (round(this * 10) / 10).toString().removeSuffix(".0")

private fun DayOfWeek.abbreviation(): String = when (this) {
    DayOfWeek.MONDAY -> "lun"
    DayOfWeek.TUESDAY -> "mar"
    DayOfWeek.WEDNESDAY -> "mié"
    DayOfWeek.THURSDAY -> "jue"
    DayOfWeek.FRIDAY -> "vie"
    DayOfWeek.SATURDAY -> "sáb"
    DayOfWeek.SUNDAY -> "dom"
}

private fun formatGmtOffset(rawOffset: String): String {
    if (rawOffset == "Z" || rawOffset == "+00:00" || rawOffset == "-00:00" || rawOffset == "+00" ||
        rawOffset == "-00"
    ) {
        return ""
    }
    var str = if (rawOffset.endsWith(":00")) rawOffset.removeSuffix(":00") else rawOffset
    if (str.length >= 3 && (str.startsWith("-0") || str.startsWith("+0"))) {
        str = str[0] + str.substring(2)
    }
    return "gmt $str"
}

internal fun timestamp(
    epochMillis: Long,
    now: Instant = Clock.System.now(),
    zone: TimeZone = TimeZone.currentSystemDefault()
): String {
    val instant = Instant.fromEpochMilliseconds(epochMillis)
    val local = instant.toLocalDateTime(zone)
    val nowLocal = now.toLocalDateTime(zone)

    val time = "${local.hour.toString().padStart(
        2,
        '0'
    )}:${local.minute.toString().padStart(2, '0')}"

    val cleanOffset = formatGmtOffset(zone.offsetAt(instant).toString())
    val offsetSuffix = if (cleanOffset.isNotBlank()) " ($cleanOffset)" else ""

    val todayDate = nowLocal.date
    val yesterdayDate = todayDate.minus(1, DateTimeUnit.DAY)

    val dateStr = when (local.date) {
        todayDate -> "Hoy"

        yesterdayDate -> "Ayer"

        else -> {
            val dow = local.dayOfWeek.abbreviation()
            val dayNum = local.day.toString().padStart(2, '0')
            val monthNum = (local.month.ordinal + 1).toString().padStart(2, '0')
            if (local.year == nowLocal.year) {
                "$dow $dayNum/$monthNum"
            } else {
                "$dow $dayNum/$monthNum/${local.year}"
            }
        }
    }

    return "$dateStr $time$offsetSuffix"
}
