package com.coffeedial.domain

import com.coffeedial.ui.AppStrings
import com.coffeedial.ui.SpanishStrings
import kotlin.math.abs
import kotlin.math.round

/** Small, explainable recipe experiments. These are not extraction diagnoses. */
data class DialInAdvice(
    val title: String,
    val explanation: String,
    val next: ShotDraft? = null,
    val references: List<Shot> = emptyList()
)

fun Shot.toDraft(): ShotDraft = ShotDraft(
    beanName = bean.name, roaster = bean.roaster, dose = dose.toString(),
    output = output.toString(), seconds = seconds.toString(), grind = grind,
    temperature = temperature?.toString().orEmpty(), milk = milk?.toString().orEmpty(),
    machine = machine.orEmpty(), notes = notes, rating = rating,
    extraWater = extraWater?.toString().orEmpty(), style = style.orEmpty(),
    cup = cup.orEmpty(), firstDrop = firstDrop?.toString().orEmpty(), taste = taste,
    dialIn = dialIn, machineId = machineId, cupId = cupId,
    grinder = grinder.orEmpty(), grinderId = grinderId, milkFoam = milkFoam
)

fun dialInAdvice(
    shot: Shot,
    context: DialInContext,
    history: List<Shot>,
    strings: AppStrings = SpanishStrings
): DialInAdvice {
    if (!context.isValid()) {
        return DialInAdvice(
            strings.adviceCheckContext,
            strings.adviceCheckContextBody
        )
    }
    if (context.method ==
        null
    ) {
        return DialInAdvice(strings.advicePickMethod, strings.advicePickMethodBody)
    }
    if (context.method !=
        BrewMethod.ESPRESSO
    ) {
        return DialInAdvice(
            strings.adviceEspressoOnly,
            strings.adviceEspressoOnlyBody
        )
    }
    if (context.roast ==
        null
    ) {
        return DialInAdvice(strings.advicePickRoast, strings.advicePickRoastBody)
    }
    if (context.goal ==
        null
    ) {
        return DialInAdvice(
            strings.advicePickGoal,
            strings.advicePickGoalBody
        )
    }
    val impression = shot.taste?.impression
        ?: return DialInAdvice(
            strings.adviceLogTaste,
            strings.adviceLogTasteBody
        )
    val additions = (shot.milk ?: 0.0) > 0 || (shot.extraWater ?: 0.0) > 0 ||
        shot.style?.lowercase() in setOf(
            "latte", "cappuccino", "capuchino", "americano", "macchiato",
            "espresso macchiato", "flat white", "cortado", "café con leche", "cafe con leche"
        )
    if (additions &&
        context.tasteBasis == null
    ) {
        return DialInAdvice(
            strings.adviceWhatTasted,
            strings.adviceWhatTastedBody
        )
    }
    if (additions && context.tasteBasis == TasteBasis.FINISHED_DRINK && shot.milk == null &&
        shot.extraWater == null
    ) {
        return DialInAdvice(
            strings.adviceLogAdditions,
            strings.adviceLogAdditionsBody
        )
    }
    val basis = context.tasteBasis ?: TasteBasis.ESPRESSO
    val nextContext = context.copy(previousShotId = shot.id, favorite = false, tasteBasis = basis)
    // Do not copy measured time, first drop, tasting notes or evaluation into a new shot.
    val draft = shot.toDraft().copy(
        seconds = "",
        firstDrop = "",
        notes = "",
        rating = 3,
        taste = null,
        dialIn = nextContext
    )
    fun normalized(value: String?) = value.orEmpty().trim().lowercase()
    fun sameMachine(candidate: Shot): Boolean {
        val left = shot.machineId?.takeIf { it.isNotBlank() }
        val right = candidate.machineId?.takeIf { it.isNotBlank() }
        if (left != null && right != null) return left == right
        val name = normalized(shot.machine)
        return name.isNotEmpty() && name == normalized(candidate.machine)
    }
    fun sameGrinder(candidate: Shot): Boolean {
        val left = shot.grinderId?.takeIf { it.isNotBlank() }
        val right = candidate.grinderId?.takeIf { it.isNotBlank() }
        if (left != null && right != null) return left == right
        val leftName = shot.grinder?.takeIf { it.isNotBlank() } ?: context.grinder
        val rightName = candidate.grinder?.takeIf { it.isNotBlank() }
            ?: candidate.dialIn?.grinder.orEmpty()
        return leftName.isNotBlank() && normalized(leftName) == normalized(rightName)
    }
    val grinderKnown = context.grinder.isNotBlank() || !shot.grinder.isNullOrBlank() ||
        !shot.grinderId.isNullOrBlank()
    val references = history.filter { candidate ->
        val c = candidate.dialIn
        candidate.id != shot.id && candidate.bean.id == shot.bean.id && c != null &&
            (
                !shot.machineId.isNullOrBlank() || !shot.machine.isNullOrBlank()
                ) && grinderKnown &&
            context.roast != RoastLevel.UNKNOWN &&
            candidate.temperature == shot.temperature &&
            normalized(candidate.style) == normalized(shot.style) &&
            (
                c.favorite ||
                    (
                        candidate.rating >= 4 &&
                            candidate.taste?.impression == TasteImpression.BALANCED
                        )
                ) &&
            c.method == BrewMethod.ESPRESSO && c.roast == context.roast && c.goal == context.goal &&
            (c.tasteBasis ?: TasteBasis.ESPRESSO) == basis &&
            sameMachine(candidate) &&
            sameGrinder(candidate) &&
            normalized(candidate.grind) == normalized(shot.grind) &&
            abs(candidate.dose - shot.dose) <= shot.dose * 0.1 &&
            abs((candidate.milk ?: 0.0) - (shot.milk ?: 0.0)) < 0.1 &&
            abs((candidate.extraWater ?: 0.0) - (shot.extraWater ?: 0.0)) < 0.1
    }.sortedWith(
        compareByDescending<Shot> {
            it.dialIn?.favorite == true
        }.thenByDescending { it.createdAt }
    ).take(5)
    fun advice(title: String, reason: String, next: ShotDraft = draft) =
        DialInAdvice(title, reason, next, references)
    fun rounded(value: Double) = (round(value * 10) / 10).toString()
    fun yieldChange(target: Double, reason: String): DialInAdvice {
        val output = target.coerceIn(shot.output * 0.75, shot.output * 1.25).coerceIn(0.1, 1000.0)
        if (abs(output - shot.output) <
            0.1
        ) {
            return advice(
                strings.adviceRepeatCompare,
                strings.adviceAtLimit
            )
        }
        return advice(
            strings.adviceTryYield(rounded(output)),
            reason + " " + strings.adviceKeepDose,
            draft.copy(output = rounded(output))
        )
    }
    if ((impression == TasteImpression.WEAK && context.goal == StrengthGoal.MILDER) ||
        (impression == TasteImpression.TOO_STRONG && context.goal == StrengthGoal.STRONGER)
    ) {
        return DialInAdvice(
            strings.adviceGoalClash,
            strings.adviceGoalClashBody
        )
    }
    if (additions && basis == TasteBasis.FINISHED_DRINK) {
        val increase =
            impression == TasteImpression.TOO_STRONG || context.goal == StrengthGoal.MILDER
        val decrease = impression == TasteImpression.WEAK || context.goal == StrengthGoal.STRONGER
        if (impression == TasteImpression.TOO_SOUR || impression == TasteImpression.TOO_BITTER) {
            return advice(
                strings.adviceEspressoAlone,
                strings.adviceEspressoAloneBody,
                draft.copy(dialIn = nextContext.copy(tasteBasis = TasteBasis.ESPRESSO))
            )
        }
        if (increase || decrease) {
            val water = (shot.extraWater ?: 0.0) > 0
            val current = if (water) shot.extraWater!! else shot.milk ?: 0.0
            val limit = if (water) 1000.0 else 200.0
            val target = (current * if (increase) 1.2 else 0.8).coerceIn(1.0, limit)
            if (abs(target - current) <
                0.1
            ) {
                return advice(
                    strings.adviceRepeatCompare,
                    strings.adviceAdditionLimit
                )
            }
            return advice(
                strings.adviceTryAddition(rounded(target), water),
                strings.adviceOneAddition,
                if (water) {
                    draft.copy(
                        extraWater = rounded(target)
                    )
                } else {
                    draft.copy(milk = rounded(target))
                }
            )
        }
        return advice(
            strings.adviceKeepRecipe,
            strings.adviceBalancedDrink
        )
    }
    if ((shot.taste.acidity ?: 0) >= 4 && (shot.taste.bitterness ?: 0) >= 4) {
        return advice(
            strings.adviceCheckConsistency,
            strings.adviceHighAcidityBitter
        )
    }
    val roastNote = strings.roastNote(context.roast)
    if (impression == TasteImpression.TOO_SOUR) {
        if (shot.ratio >= 3.5 &&
            !grinderKnown
        ) {
            return DialInAdvice(
                strings.adviceConfirmGrinder,
                strings.adviceLongSour
            )
        }
        if (shot.ratio >=
            3.5
        ) {
            return advice(
                strings.adviceSlightlyFiner,
                roastNote +
                    strings.adviceAlreadyLong,
                draft.copy(grind = "")
            )
        }
        return yieldChange(
            shot.output * if (context.roast == RoastLevel.LIGHT) 1.15 else 1.1,
            roastNote +
                strings.adviceMoreYieldSour
        )
    }
    if (impression == TasteImpression.TOO_BITTER) {
        if (shot.ratio <= 1.2 &&
            !grinderKnown
        ) {
            return DialInAdvice(
                strings.adviceConfirmGrinder,
                strings.adviceShortBitter
            )
        }
        if (shot.ratio <=
            1.2
        ) {
            return advice(
                strings.adviceSlightlyCoarser,
                roastNote +
                    strings.adviceAlreadyShort,
                draft.copy(grind = "")
            )
        }
        return yieldChange(
            shot.output * if (context.roast == RoastLevel.DARK) 0.95 else 0.9,
            roastNote +
                strings.adviceLessYieldBitter
        )
    }
    val stronger = impression == TasteImpression.WEAK || context.goal == StrengthGoal.STRONGER
    val milder = impression == TasteImpression.TOO_STRONG || context.goal == StrengthGoal.MILDER
    val personal = references.takeIf { it.size >= 2 }?.map { it.ratio }?.sorted()?.let {
        it[
            it.size /
                2
        ] *
            shot.dose
    }
    if (stronger) {
        val ratio = when (context.roast) {
            RoastLevel.LIGHT -> 2.5
            RoastLevel.DARK -> 2.0
            else -> 2.25
        }
        val target = minOf(shot.output * 0.9, personal ?: shot.dose * ratio)
        return yieldChange(
            target,
            if (personal !=
                null
            ) {
                strings.advicePersonalStronger(references.size)
            } else {
                strings.adviceStrongerYield
            }
        )
    }
    if (milder) {
        return yieldChange(
            maxOf(shot.output * 1.1, personal ?: shot.output * 1.15),
            if (personal !=
                null
            ) {
                strings.advicePersonalMilder
            } else {
                strings.adviceMilderYield
            }
        )
    }
    return advice(
        strings.adviceKeepRecipe,
        strings.adviceKeepBalanced
    )
}

/** Parent links may outlive deleted shots; never loop on malformed imported graphs. */
fun dialInLineage(shot: Shot, history: List<Shot>): List<Shot> {
    val byId = history.associateBy { it.id }
    val seen = mutableSetOf<String>()
    val result = mutableListOf<Shot>()
    var current: Shot? = shot
    while (current != null && seen.add(current.id) && result.size < 100) {
        result += current
        current = current.dialIn?.previousShotId?.let(byId::get)
    }
    return result.reversed()
}
