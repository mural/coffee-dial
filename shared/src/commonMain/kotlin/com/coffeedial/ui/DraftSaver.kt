package com.coffeedial.ui

import androidx.compose.runtime.saveable.Saver
import com.coffeedial.domain.DialInContext
import com.coffeedial.domain.MilkFoam
import com.coffeedial.domain.ShotDraft
import com.coffeedial.domain.TasteProfile
import kotlinx.serialization.json.Json

internal val DraftSaver = Saver<ShotDraft, List<String>>(
    save = {
        listOf(
            it.beanName, it.roaster, it.dose, it.output, it.seconds,
            it.grind, it.temperature, it.milk, it.machine, it.notes, it.rating.toString(),
            it.extraWater, it.style, it.cup, it.firstDrop,
            it.taste?.let { value -> Json.encodeToString(value) }.orEmpty(),
            it.dialIn?.let { value -> Json.encodeToString(value) }.orEmpty(),
            it.machineId.orEmpty(),
            it.cupId.orEmpty(),
            it.grinder,
            it.grinderId.orEmpty(),
            it.milkFoam?.name.orEmpty()
        )
    },
    restore = {
        ShotDraft(
            it[0], it[1], it[2], it[3], it[4], it[5],
            it[6], it[7], it[8], it[9], it[10].toInt(),
            it.getOrElse(11) { "" },
            it.getOrElse(12) { "" }, it.getOrElse(13) { "" }, it.getOrElse(14) { "" },
            it.getOrNull(15)?.takeIf { value -> value.isNotEmpty() }?.let { value ->
                Json.decodeFromString<TasteProfile>(value)
            },
            it.getOrNull(16)?.takeIf { value -> value.isNotEmpty() }?.let { value ->
                Json.decodeFromString<DialInContext>(value)
            },
            it.getOrNull(17)?.takeIf { value -> value.isNotEmpty() },
            it.getOrNull(18)?.takeIf { value -> value.isNotEmpty() },
            grinder = it.getOrElse(19) { "" },
            grinderId = it.getOrNull(20)?.takeIf { value -> value.isNotEmpty() },
            milkFoam = it.getOrNull(21)?.takeIf { value ->
                value.isNotEmpty()
            }?.let(MilkFoam::valueOf)
        )
    }
)
