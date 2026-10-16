package com.coffeedial.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import com.coffeedial.ui.CoffeeButton as Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.coffeedial.domain.MilkFoam
import com.coffeedial.domain.PrepareBaseDose
import com.coffeedial.domain.PreparedAmounts
import com.coffeedial.domain.amountsForDose
import com.coffeedial.domain.cupProfileFor
import com.coffeedial.domain.doseChoices
import com.coffeedial.domain.prepareDrinks
import kotlin.math.roundToInt

@Composable
internal fun PrepareCoffeeSection(onPrepare: (PreparedAmounts) -> Unit) {
    val strings = LocalStrings.current
    var selectedName by rememberSaveable { mutableStateOf<String?>(null) }
    var doseGrams by rememberSaveable { mutableIntStateOf(PrepareBaseDose.toInt()) }
    var foamName by rememberSaveable { mutableStateOf<String?>(null) }
    val drink = prepareDrinks.firstOrNull { it.name == selectedName }
    val doses = drink?.doseChoices()
    LaunchedEffect(drink?.name, doses?.first, doses?.last) {
        foamName = null
        doses?.let { doseGrams = doseGrams.coerceIn(it.first, it.last) }
    }
    val dose = doses?.let { doseGrams.coerceIn(it.first, it.last) }
    val foam = foamName?.let { value ->
        MilkFoam.entries.firstOrNull { it.name == value }
    } ?: drink?.milkFoam
    val amounts = if (drink != null && dose != null) drink.amountsForDose(dose, foam) else null
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text(strings.prepareTitle, style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            prepareDrinks.forEach { option ->
                FilterChip(
                    selected = option.name == selectedName,
                    onClick = { selectedName = option.name },
                    label = { Text(strings.knownDrinkName(option.name)) }
                )
            }
        }
        if (drink != null && amounts != null) {
            val profile = cupProfileFor(amounts.output + amounts.water + amounts.milk)
            DrinkCupChart(
                dose = amounts.dose,
                output = amounts.output,
                water = amounts.water,
                milk = amounts.milk,
                capacityCm3 = profile.capacityCm3,
                milkFoam = amounts.milkFoam,
                foamNote = amounts.milkFoam?.let { strings.foamHowToText(drink.name, it) },
                onFoamClick = if (amounts.milk > 0.0) {
                    {
                        val order = MilkFoam.entries
                        val index = order.indexOf(amounts.milkFoam ?: MilkFoam.NONE)
                        foamName = order[(index + 1) % order.size].name
                    }
                } else {
                    null
                }
            )
            if (doses != null && dose != null) {
                Text(
                    strings.doseSizeLabel(dose.toString()),
                    style = MaterialTheme.typography.labelMedium
                )
                Slider(
                    value = dose.toFloat(),
                    onValueChange = {
                        doseGrams = it.roundToInt().coerceIn(doses.first, doses.last)
                    },
                    valueRange = doses.first.toFloat()..doses.last.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )
                ReferenceDoseMark(PrepareBaseDose.toInt(), doses.first, doses.last)
                val doubleDose = PrepareBaseDose.toInt() * 2
                if (doses.last < doubleDose) {
                    Text(
                        strings.maxDoseNotice(doses.last.toString()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Button(
                onClick = { onPrepare(amounts) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(strings.logWithMeasurements)
            }
        }
    }
}

internal fun foamHowTo(
    drink: String,
    foam: MilkFoam,
    strings: AppStrings = SpanishStrings
): String = strings.foamHowToText(drink, foam)

/** Tick under the slider at the reference dose. Thumb width matches Material3's handle. */
@Composable
private fun ReferenceDoseMark(reference: Int, min: Int, max: Int) {
    if (reference !in min..max || max <= min) return
    val fraction = (reference - min).toFloat() / (max - min)
    val color = MaterialTheme.colorScheme.primary
    Box(Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.layout { measurable, constraints ->
                val placeable = measurable.measure(
                    constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity)
                )
                val thumb = 4.dp.roundToPx()
                val width = constraints.maxWidth
                val center = thumb / 2 + ((width - thumb) * fraction).roundToInt()
                val x = (center - placeable.width / 2)
                    .coerceIn(0, (width - placeable.width).coerceAtLeast(0))
                layout(width, placeable.height) { placeable.placeRelative(x, 0) }
            }
        ) {
            Box(Modifier.width(2.dp).height(8.dp).background(color))
            Text("1×", style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}
