package com.coffeedial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeedial.domain.Shot
import com.coffeedial.domain.analyzeHistory
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun AnalysisScreen(shots: List<Shot>) {
    val data = remember(shots) { analyzeHistory(shots) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Análisis", style = MaterialTheme.typography.headlineSmall)
            Text("Todo tu historial · ${data.count} shots")
        }
        if (data.count == 0) {
            item {
                Text(
                    "Registrá tu primer shot para ver tus promedios y descubrir tus cafés más usados."
                )
            }
        } else {
            item {
                AnalysisCard("Tu café en números") {
                    Metric("Puntuación media", "${data.averageRating?.pretty()} / 5")
                    Metric("Cafés diferentes", "${data.coffees.size}")
                    Metric("Café utilizado", "${data.totalDose.pretty()} g")
                }
            }
            item {
                AnalysisCard("Receta promedio") {
                    Metric("Dosis", "${data.averageDose?.pretty()} g")
                    Metric("Salida", "${data.averageOutput?.pretty()} g")
                    Metric("Tiempo", "${data.averageSeconds?.pretty()} s")
                    Metric("Ratio medio por shot", "1:${data.averageRatio?.pretty()}")
                    Text(
                        "Promedios de tus registros; no son una recomendación de receta.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            item {
                AnalysisCard("Cómo salieron tus shots") {
                    (5 downTo 1).forEach { score ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            CoffeeRating(score)
                            Text("${data.ratings.getValue(score)} shots")
                        }
                        LinearProgressIndicator(
                            progress = { data.ratings.getValue(score).toFloat() / data.count },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            item {
                AnalysisCard("Cafés más usados") {
                    data.coffees.take(5).forEach { coffee ->
                        Column {
                            Text(
                                "${coffee.name} · ${coffee.count} shots",
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
