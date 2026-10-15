package com.coffeedial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.coffeedial.ui.CoffeeButton as Button
import com.coffeedial.ui.CoffeeCard as Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import com.coffeedial.ui.CoffeeTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeedial.domain.BrewMethod
import com.coffeedial.domain.DialInContext
import com.coffeedial.domain.RoastLevel
import com.coffeedial.domain.Shot
import com.coffeedial.domain.ShotDraft
import com.coffeedial.domain.StrengthGoal
import com.coffeedial.domain.TasteBasis
import com.coffeedial.domain.dialInAdvice
import com.coffeedial.domain.dialInLineage
import kotlinx.serialization.json.Json

private val ContextSaver = Saver<DialInContext, String>(
    save = { Json.encodeToString(it) },
    restore = { Json.decodeFromString(it) }
)

internal fun RoastLevel.label(strings: AppStrings): String = when (this) {
    RoastLevel.LIGHT -> strings.roastLight
    RoastLevel.MEDIUM -> strings.roastMedium
    RoastLevel.DARK -> strings.roastDark
    RoastLevel.UNKNOWN -> strings.roastUnknown
}
internal fun BrewMethod.label(strings: AppStrings): String = when (this) {
    BrewMethod.ESPRESSO -> "Espresso"
    BrewMethod.FILTER -> strings.methodFilter
    BrewMethod.MOKA -> strings.methodMoka
    BrewMethod.OTHER -> strings.methodOther
}
internal fun StrengthGoal.label(strings: AppStrings): String = when (this) {
    StrengthGoal.STRONGER -> strings.goalStronger
    StrengthGoal.BALANCED -> strings.goalBalanced
    StrengthGoal.MILDER -> strings.goalMilder
}
internal fun TasteBasis.label(strings: AppStrings): String = when (this) {
    TasteBasis.ESPRESSO -> strings.basisEspresso
    TasteBasis.FINISHED_DRINK -> strings.basisFinishedDrink
}

@Composable
fun DialInContextEditor(value: DialInContext, enabled: Boolean, onChange: (DialInContext) -> Unit) {
    val strings = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(strings.navBrew, style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BrewMethod.entries.forEach { item ->
                FilterChip(value.method == item, {
                    onChange(value.copy(method = item))
                }, enabled = enabled, label = { Text(item.label(strings)) })
            }
        }
        Text(strings.roastLevel, style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RoastLevel.entries.forEach { item ->
                FilterChip(value.roast == item, {
                    onChange(value.copy(roast = item))
                }, enabled = enabled, label = { Text(item.label(strings)) })
            }
        }
        OutlinedTextField(
            value.grinder,
            { onChange(value.copy(grinder = it.take(200))) },
            label = {
                Text(strings.grinder)
            },
            enabled = enabled,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Text(strings.strengthGoal, style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StrengthGoal.entries.forEach { item ->
                FilterChip(value.goal == item, {
                    onChange(value.copy(goal = item))
                }, enabled = enabled, label = { Text(item.label(strings)) })
            }
        }
        Text(strings.flavorProfile, style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TasteBasis.entries.forEach { item ->
                FilterChip(value.tasteBasis == item, {
                    onChange(value.copy(tasteBasis = item))
                }, enabled = enabled, label = { Text(item.label(strings)) })
            }
        }
    }
}

@Composable
fun DialInFormContext(value: DialInContext?, enabled: Boolean, onChange: (DialInContext?) -> Unit) {
    val strings = LocalStrings.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column {
        if (value?.previousShotId !=
            null
        ) {
            Text(strings.linkedAttempt)
        }
        TextButton(onClick = { expanded = !expanded }, enabled = enabled) {
            Text(
                if (expanded) strings.hideDialContext else strings.showDialContext
            )
        }
        if (expanded) DialInContextEditor(value ?: DialInContext(), enabled) { onChange(it) }
    }
}

@Composable
fun DialInScreen(
    shot: Shot,
    history: List<Shot>,
    saving: Boolean,
    error: String?,
    onEdit: () -> Unit,
    onFavorite: (DialInContext) -> Unit,
    onSelect: (Shot) -> Unit,
    onNext: (ShotDraft, DialInContext) -> Unit
) {
    val strings = LocalStrings.current
    var context by rememberSaveable(shot.id, shot.dialIn, shot.grinder, stateSaver = ContextSaver) {
        val stored = shot.dialIn ?: DialInContext()
        mutableStateOf(
            if (stored.grinder.isBlank() && !shot.grinder.isNullOrBlank()) {
                stored.copy(grinder = shot.grinder.orEmpty())
            } else {
                stored
            }
        )
    }
    val advice = dialInAdvice(shot, context, history, strings)
    Column(
        Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(strings.adjustEspresso, style = MaterialTheme.typography.headlineLarge)
        Text(shot.bean.name, style = MaterialTheme.typography.titleLarge)
        Text(
            "${shot.dose.pretty()} g -> ${shot.output.pretty()} g · ${shot.seconds.pretty()} s · 1:${shot.ratio.pretty()}"
        )
        Text(
            "${strings.machine}: ${shot.machine ?: strings.notRecorded} · " +
                "${strings.grind}: ${strings.grindLevelLabel(shot.grind)}"
        )
        Text(
            "${strings.flavorProfile}: " +
                (shot.taste?.impression?.label(strings) ?: strings.notRated)
        )
        TextButton(onClick = onEdit, enabled = !saving) { Text(strings.editShotTaste) }
        DialInContextEditor(context, !saving) { context = it }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(strings.nextExtractionTitle, style = MaterialTheme.typography.titleMedium)
                Text(advice.title, style = MaterialTheme.typography.titleLarge)
                Text(advice.explanation)
                advice.next?.let { next ->
                    Text(strings.doseYieldLine(next.dose, next.output))
                    if (next.milk.isNotBlank()) Text(strings.milkLine(next.milk))
                    if (next.extraWater.isNotBlank()) Text(strings.waterLine(next.extraWater))
                    if (next.grind.isBlank()) {
                        Text(
                            strings.noteGrindInForm
                        )
                    }
                    Text(strings.measureAndLogAttempt)
                    Button(onClick = {
                        onNext(next, context)
                    }, enabled = !saving) { Text(strings.prepareNextAttempt) }
                }
            }
        }
        Text(
            strings.compareFromPump,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            strings.tasteNotTime,
            style = MaterialTheme.typography.bodySmall
        )
        TextButton(onClick = { onFavorite(context) }, enabled = !saving) {
            Text(
                if (shot.dialIn?.favorite == true) {
                    strings.removeFavorite
                } else {
                    strings.markFavorite
                }
            )
        }
        if (advice.references.isNotEmpty()) {
            Text(
                strings.referencesCount(advice.references.size),
                style = MaterialTheme.typography.titleMedium
            )
            advice.references.forEach { reference ->
                TextButton(onClick = { onSelect(reference) }, enabled = !saving) {
                    Text(
                        "${reference.dose.pretty()} -> ${reference.output.pretty()} g · " +
                            "${reference.rating}/5" +
                            if (reference.dialIn?.favorite == true) {
                                " · ${strings.favoriteLabel}"
                            } else {
                                ""
                            }
                    )
                }
            }
        } else {
            Text(
                strings.refsWillAppear
            )
        }
        Text(strings.evolution, style = MaterialTheme.typography.titleMedium)
        val lineage = dialInLineage(shot, history)
        if (lineage.firstOrNull()?.dialIn?.previousShotId !=
            null
        ) {
            Text(strings.previousAttemptGone)
        }
        lineage.forEachIndexed { index, item ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        strings.attemptNumber(index + 1, item.dialIn?.favorite == true)
                    )
                    Text(
                        "${item.dose.pretty()} g -> ${item.output.pretty()} g -> ${item.seconds.pretty()} s"
                    )
                    Text(
                        strings.tasteRating(
                            item.taste?.impression?.label(strings) ?: strings.notRated,
                            item.rating
                        )
                    )
                    if (item.id !=
                        shot.id
                    ) {
                        TextButton(onClick = {
                            onSelect(item)
                        }, enabled = !saving) { Text(strings.viewAttempt) }
                    }
                }
            }
        }
        history.filter {
            it.dialIn?.previousShotId == shot.id
        }.sortedBy { it.createdAt }.forEach { child ->
            TextButton(onClick = { onSelect(child) }, enabled = !saving) {
                Text(strings.nextAttemptLine(child.output.pretty(), child.seconds.pretty()))
            }
        }
        error?.let { Text(strings.userMessage(it), color = MaterialTheme.colorScheme.error) }
    }
}
