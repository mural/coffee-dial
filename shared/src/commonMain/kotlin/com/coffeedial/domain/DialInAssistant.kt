package com.coffeedial.domain

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
    grinder = grinder.orEmpty(), grinderId = grinderId
)

fun dialInAdvice(shot: Shot, context: DialInContext, history: List<Shot>): DialInAdvice {
    if (!context.isValid()) {
        return DialInAdvice(
            "Revisá el contexto",
            "El nombre del molinillo admite hasta 200 caracteres."
        )
    }
    if (context.method ==
        null
    ) {
        return DialInAdvice("Elegí la preparación", "Confirmá cómo preparaste este café.")
    }
    if (context.method !=
        BrewMethod.ESPRESSO
    ) {
        return DialInAdvice(
            "Por ahora, espresso",
            "Este asistente ajusta espresso. No aplica estas recetas a filtro o moka."
        )
    }
    if (context.roast ==
        null
    ) {
        return DialInAdvice("Indicá el tueste", "Si no lo sabés, elegí Desconocido.")
    }
    if (context.goal ==
        null
    ) {
        return DialInAdvice(
            "Elegí tu objetivo",
            "Podés buscar más intensidad, más suavidad o mantener el equilibrio."
        )
    }
    val impression = shot.taste?.impression
        ?: return DialInAdvice(
            "Primero, registrá el sabor",
            "Editá el shot y elegí cómo salió en Perfil de sabor. El tiempo y el ratio solos no alcanzan."
        )
    val additions = (shot.milk ?: 0.0) > 0 || (shot.extraWater ?: 0.0) > 0 ||
        shot.style?.lowercase() in
        setOf("latte", "cappuccino", "capuchino", "americano", "macchiato", "flat white")
    if (additions &&
        context.tasteBasis == null
    ) {
        return DialInAdvice(
            "¿Qué probaste?",
            "Separá el sabor del espresso solo del de la bebida con agua o leche."
        )
    }
    if (additions && context.tasteBasis == TasteBasis.FINISHED_DRINK && shot.milk == null &&
        shot.extraWater == null
    ) {
        return DialInAdvice(
            "Registrá agua y leche",
            "Completá las cantidades agregadas antes de ajustar la bebida final."
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
                "Repetí y compará",
                "La receta ya está en el límite de este ajuste. Repetí y evaluá el sabor antes de cambiar otra variable."
            )
        }
        return advice(
            "Probar ${rounded(output)} g de espresso",
            reason + " Mantené dosis, molienda y temperatura; medí el nuevo tiempo.",
            draft.copy(output = rounded(output))
        )
    }
    if ((impression == TasteImpression.WEAK && context.goal == StrengthGoal.MILDER) ||
        (impression == TasteImpression.TOO_STRONG && context.goal == StrengthGoal.STRONGER)
    ) {
        return DialInAdvice(
            "Revisá sabor y objetivo",
            "La valoración y la intensidad buscada apuntan en direcciones opuestas. Confirmalas antes de cambiar la receta."
        )
    }
    if (additions && basis == TasteBasis.FINISHED_DRINK) {
        val increase =
            impression == TasteImpression.TOO_STRONG || context.goal == StrengthGoal.MILDER
        val decrease = impression == TasteImpression.WEAK || context.goal == StrengthGoal.STRONGER
        if (impression == TasteImpression.TOO_SOUR || impression == TasteImpression.TOO_BITTER) {
            return advice(
                "Probá el espresso solo",
                "El agua y la leche cambian la percepción. Repetí la receta y evaluá el espresso antes de agregarlos; no hay base para diagnosticar la extracción.",
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
                    "Repetí y compará",
                    "La cantidad agregada está en el límite admitido. Probá el espresso solo antes de modificar la extracción."
                )
            }
            return advice(
                "Probar ${rounded(target)} ml de ${if (water) "agua" else "leche"}",
                "Para cambiar la intensidad de la bebida final, ajustá primero una sola adición. Conservá la receta del espresso.",
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
            "Conservá la receta",
            "La bebida te resultó equilibrada. Repetila para comprobar consistencia y marcala como favorita si la preferís."
        )
    }
    if ((shot.taste.acidity ?: 0) >= 4 && (shot.taste.bitterness ?: 0) >= 4) {
        return advice(
            "Revisá la consistencia",
            "Acidez y amargor altos pueden tener varias causas. Repetí con distribución pareja del café antes de cambiar la receta; no podemos concluir subextracción o sobreextracción."
        )
    }
    val roastNote = when (context.roast) {
        RoastLevel.LIGHT -> "En tuestes claros puede haber acidez propia del café. "
        RoastLevel.DARK -> "En tuestes oscuros puede haber amargor propio del tueste. "
        RoastLevel.UNKNOWN -> "Sin conocer el tueste, este es un ensayo conservador. "
        else -> ""
    }
    if (impression == TasteImpression.TOO_SOUR) {
        if (shot.ratio >= 3.5 &&
            !grinderKnown
        ) {
            return DialInAdvice(
                "Confirmá el molinillo",
                "Con un yield largo y sabor ácido, conviene revisar distribución y molienda. Indicá tu molinillo antes de probar un ajuste."
            )
        }
        if (shot.ratio >=
            3.5
        ) {
            return advice(
                "Probar un ajuste apenas más fino",
                roastNote +
                    "Ya usás un yield largo. Si tu molinillo permite regularlo, cambiá un paso pequeño hacia fino y mantené dosis y yield. Registrá el ajuste real; no hay un número universal de clicks.",
                draft.copy(grind = "")
            )
        }
        return yieldChange(
            shot.output * if (context.roast == RoastLevel.LIGHT) 1.15 else 1.1,
            roastNote +
                "Como el sabor fue demasiado ácido, probá un poco más de yield. También puede bajar la intensidad: priorizá evaluar el sabor en este intento."
        )
    }
    if (impression == TasteImpression.TOO_BITTER) {
        if (shot.ratio <= 1.2 &&
            !grinderKnown
        ) {
            return DialInAdvice(
                "Confirmá el molinillo",
                "Con un yield ya corto, revisá molienda y distribución. Indicá tu molinillo antes de intentar cambiarlo."
            )
        }
        if (shot.ratio <=
            1.2
        ) {
            return advice(
                "Probar un ajuste apenas más grueso",
                roastNote +
                    "El yield ya es corto. Si podés regular la molienda, probá un paso pequeño hacia grueso manteniendo dosis y yield; compará el sabor.",
                draft.copy(grind = "")
            )
        }
        return yieldChange(
            shot.output * if (context.roast == RoastLevel.DARK) 0.95 else 0.9,
            roastNote +
                "Probá un yield algo menor y compará el amargor. Esto puede aumentar la concentración; no demuestra sobreextracción."
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
                "Uso el ratio central de ${references.size} recetas comparables que preferiste como referencia, sin cambiar todo a la vez."
            } else {
                "Para buscar más intensidad, probá menos yield con la misma dosis. Compará también si aparece acidez."
            }
        )
    }
    if (milder) {
        return yieldChange(
            maxOf(shot.output * 1.1, personal ?: shot.output * 1.15),
            if (personal !=
                null
            ) {
                "Tus recetas preferidas comparables sirven como referencia para un yield mayor."
            } else {
                "Para un espresso más suave, probá algo más de yield y compará si aumenta el amargor."
            }
        )
    }
    return advice(
        "Conservá la receta",
        "Lo registraste equilibrado y querés mantenerlo. Repetilo para comprobar consistencia y marcá tu preferido como favorito."
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
