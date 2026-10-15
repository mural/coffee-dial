package com.coffeedial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeedial.domain.amounts
import com.coffeedial.domain.cupProfileFor
import com.coffeedial.domain.prepareDrinks
import org.jetbrains.compose.ui.tooling.preview.Preview

private val extraRatioTips = drinkRatioTips.filter { tip ->
    prepareDrinks.none { it.waterParts == tip.water && it.milkParts == tip.milk }
}

@Composable
fun TipsScreen() {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val strings = LocalStrings.current
    val tabs = listOf(strings.tabStyles, strings.tabTimes, strings.tabGrains)
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(strings.tipsTitle, style = MaterialTheme.typography.headlineSmall)
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            fontWeight = if (selectedTab == index) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            }
                        )
                    }
                )
            }
        }
        when (selectedTab) {
            0 -> StylesTabContent(Modifier.weight(1f))
            1 -> TimesTabContent(Modifier.weight(1f))
            2 -> GrainsTabContent(Modifier.weight(1f))
        }
    }
}

@Composable
private fun StylesTabContent(modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    var drinkName by rememberSaveable { mutableStateOf(prepareDrinks.first().name) }
    val drink = prepareDrinks.firstOrNull { it.name == drinkName } ?: prepareDrinks.first()
    val amounts = drink.amounts(1.0)
    LazyColumn(
        modifier,
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(strings.tipsSplitTitle, style = MaterialTheme.typography.titleMedium)
            Text(
                strings.tipsSplitBody,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item { Text(strings.extractionHeading, style = MaterialTheme.typography.titleLarge) }
        items(strings.extractionGuides()) { tip ->
            TipCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(tip.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        tip.detail,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(tip.body, style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            Text(strings.drinksHeading, style = MaterialTheme.typography.titleLarge)
            Text(
                strings.drinksIntro,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                prepareDrinks.forEach { option ->
                    FilterChip(
                        selected = option.name == drink.name,
                        onClick = { drinkName = option.name },
                        label = { Text(strings.knownDrinkName(option.name)) }
                    )
                }
            }
        }
        if (amounts != null) {
            item {
                val volume = amounts.output + amounts.water + amounts.milk
                TipCard {
                    Text(
                        strings.knownDrinkName(drink.name),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        strings.coffeeWaterMilkRatio,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        strings.ratioParts(
                            drink.waterParts.pretty(),
                            drink.milkParts.pretty()
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        strings.doseCoffeeExtraction(
                            amounts.dose.pretty(),
                            amounts.output.pretty(),
                            drink.extractionRatio.pretty()
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    DrinkCupChart(
                        dose = amounts.dose,
                        output = amounts.output,
                        water = amounts.water,
                        milk = amounts.milk,
                        capacityCm3 = cupProfileFor(volume).capacityCm3,
                        milkFoam = amounts.milkFoam,
                        foamNote = amounts.milkFoam?.let { foamHowTo(drink.name, it, strings) }
                    )
                }
            }
        }
        item {
            Text(strings.otherMixesHeading, style = MaterialTheme.typography.titleLarge)
            Text(
                strings.otherMixesBody,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(extraRatioTips) { recipe ->
            TipCard {
                Text(
                    strings.knownDrinkName(recipe.name),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    strings.ratioParts(recipe.water.pretty(), recipe.milk.pretty()),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    strings.waterAndMilkMl(
                        (36 * recipe.water).pretty(),
                        (36 * recipe.milk).pretty()
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            Text(
                strings.macchiatoFootnote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TimesTabContent(modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    LazyColumn(
        modifier,
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(strings.timeHeading, style = MaterialTheme.typography.titleLarge)
            Text(
                strings.timeIntro,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(strings.timeGuides()) { tip ->
            TipCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(tip.range, style = MaterialTheme.typography.titleMedium)
                    Text(
                        tip.trend,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    strings.flowAndTaste(tip.flow, tip.taste),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    strings.whatToTry(tip.advice),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun GrainsTabContent(modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    LazyColumn(
        modifier,
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(strings.grainsHeading, style = MaterialTheme.typography.titleLarge)
            Text(
                strings.grainsIntro,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(strings.grainGuides()) { tip ->
            TipCard {
                Text(tip.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    tip.detail,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(tip.body, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun TipCard(content: @Composable ColumnScope.() -> Unit) {
    CoffeeCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun TipsScreenPreview() {
    MaterialTheme { TipsScreen() }
}
