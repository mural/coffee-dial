package com.coffeedial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeedial.domain.Shot
import com.coffeedial.domain.analyzeHistory
import com.coffeedial.ui.CoffeeCard as Card
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun AnalysisScreen(shots: List<Shot>) {
    val strings = LocalStrings.current
    val data = remember(shots) { analyzeHistory(shots) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text(strings.navAnalysis, style = MaterialTheme.typography.headlineSmall)
            Text(strings.shotCount(data.count))
        }
        if (data.count == 0) {
            item {
                Text(
                    strings.homeNoShotsYet
                )
            }
        } else {
            item {
                AnalysisCard(strings.coffeeInNumbers) {
                    Metric(strings.averageRating, "${data.averageRating?.pretty()} / 5")
                    Metric(strings.differentCoffees, "${data.coffees.size}")
                    Metric(strings.totalCoffeeUsed, "${data.totalDose.pretty()} g")
                }
            }
            item {
                AnalysisCard(strings.averageRecipe) {
                    Metric(strings.dose, "${data.averageDose?.pretty()} g")
                    Metric(strings.yield, "${data.averageOutput?.pretty()} g")
                    Metric(strings.time, "${data.averageSeconds?.pretty()} s")
                    Metric(strings.averageRatio, "1:${data.averageRatio?.pretty()}")
                }
            }
            item {
                AnalysisCard(strings.shotBreakdownTitle) {
                    (5 downTo 1).forEach { score ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            CoffeeRating(score)
                            Text(strings.shotCount(data.ratings.getValue(score)))
                        }
                        LinearProgressIndicator(
                            progress = { data.ratings.getValue(score).toFloat() / data.count },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            item {
                AnalysisCard(strings.topCoffees) {
                    data.coffees.take(5).forEach { coffee ->
                        Column {
                            Text(
                                "${coffee.name} · ${strings.shotCount(coffee.count)}",
                                style = MaterialTheme.typography.titleSmall
                            )
                            if (coffee.roaster.isNotBlank()) Text(coffee.roaster)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalysisCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Preview(showBackground = true)
@Composable
private fun AnalysisPreview() {
    MaterialTheme { AnalysisScreen(listOf(mockShot)) }
}
