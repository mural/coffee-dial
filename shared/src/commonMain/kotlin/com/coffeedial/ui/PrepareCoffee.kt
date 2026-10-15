package com.coffeedial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import com.coffeedial.ui.CoffeeButton as Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeedial.domain.CupProfile
import com.coffeedial.domain.MilkFoam
import com.coffeedial.domain.PreparedAmounts
import com.coffeedial.domain.amounts
import com.coffeedial.domain.cupProfileFor
import com.coffeedial.domain.maxSize
import com.coffeedial.domain.prepareDrinks
import kotlin.math.roundToInt

@Composable
internal fun PrepareCoffeeSection(onPrepare: (PreparedAmounts) -> Unit) {
    val strings = LocalStrings.current
    var selectedName by rememberSaveable { mutableStateOf<String?>(null) }
    var size by rememberSaveable { mutableFloatStateOf(1f) }
    var foamName by rememberSaveable { mutableStateOf<String?>(null) }
    val drink = prepareDrinks.firstOrNull { it.name == selectedName }
    val maxSize = drink?.maxSize() ?: 2.0
    LaunchedEffect(drink?.name, maxSize) {
        foamName = null
        if (size > maxSize) size = maxSize.toFloat()
    }
    val current = size.toDouble().coerceIn(0.5, maxSize)
    val foam = foamName?.let { value ->
        MilkFoam.entries.firstOrNull { it.name == value }
    } ?: drink?.milkFoam
    val amounts = drink?.amounts(current, foam = foam)
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
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CupProfile.entries.forEach { option ->
                    FilterChip(
                        selected = option == profile,
                        onClick = {},
                        label = { Text("${option.label(strings)} · ${option.capacityCm3.pretty()} cm³") }
                    )
                }
            }
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
            Text(strings.sizeLabel(current.pretty()), style = MaterialTheme.typography.labelMedium)
            Slider(
                value = current.toFloat(),
                onValueChange = { size = (it * 10).roundToInt() / 10f },
                valueRange = 0.5f..maxSize.toFloat(),
                steps = sizeSteps(maxSize)
            )
            if (maxSize < 2.0) {
                Text(
                    strings.maxSizeNotice(maxSize.pretty()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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

private fun sizeSteps(maxSize: Double): Int {
    val spans = ((maxSize - 0.5) / 0.1).roundToInt() - 1
    return spans.coerceAtLeast(0)
}
