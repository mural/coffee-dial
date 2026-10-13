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

private data class TimeTip(
    val timeRange: String,
    val flow: String,
    val trend: String,
    val taste: String,
    val advice: String
)

private val timeTipsList = listOf(
    TimeTip(
        "<15 s",
        "Muy rápido",
        "Muy subextraído",
        "Aguado, ácido, poco cuerpo",
        "Molienda mucho más fina"
    ),
    TimeTip(
        "15-20 s",
        "Rápido",
        "Subextraído",
        "Ácido, poco dulce, final corto",
        "Molienda más fina"
    ),
    TimeTip(
        "20-25 s",
        "Algo rápido",
        "Puede estar subextraído",
        "Ya puede ser rico, depende del grano",
        "Algo más fino si le falta cuerpo"
    ),
    TimeTip(
        "25-30 s",
        "Normal",
        "Zona clásica",
        "Balance, dulzor y cuerpo",
        "Ajustar fino según el gusto"
    ),
    TimeTip(
        "30-35 s",
        "Lento",
        "Puede estar perfecto",
        "Más cuerpo e intensidad",
        "Si está rico, no tocar nada"
    ),
    TimeTip(
        "35-40 s",
        "Muy lento",
        "Tendencia a sobreextracción",
        "Amargo, seco, astringente",
        "Molienda algo más gruesa"
    ),
    TimeTip(
        ">40 s",
        "Goteo",
        "Probable sobreextracción",
        "Muy amargo, seco, áspero",
        "Molienda más gruesa"
    )
)

private data class ExtractionTip(val title: String, val range: String, val note: String)

private val extractionTips = listOf(
    ExtractionTip(
        "Ristretto",
        "Hasta 1:1.5",
        "Shot corto. Más cuerpo y dulzor, menos amargor."
    ),
    ExtractionTip(
        "Espresso",
        "Más de 1:1.5 y hasta 1:2.5",
        "La zona habitual. Cuerpo, acidez y dulzor en balance."
    ),
    ExtractionTip(
        "Espresso largo",
        "Más de 1:2.5 y hasta 1:3",
        "Un poco más de agua en la extracción. Más liviano."
    ),
    ExtractionTip(
        "Lungo",
        "Más de 1:3",
        "Más agua durante la extracción. No es un americano."
    )
)

private data class GrainTip(val name: String, val type: String, val profile: String)

private val grainTipsList = listOf(
    GrainTip(
        "Arábica y robusta",
        "Especies",
        "Arábica: más aroma, acidez y dulzor. Robusta: más cuerpo, crema y amargor."
    ),
    GrainTip(
        "Bourbon, Caturra, Catuaí",
        "Varietales clásicos",
        "Cuerpo sedoso, chocolate, caramelo, frutas rojas y acidez cítrica."
    ),
    GrainTip(
        "Geisha",
        "Varietal de alta gama",
        "Floral, acidez cítrica brillante, lima y cuerpo ligero."
    ),
    GrainTip(
        "Pacamara y Maragogipe",
        "Granos grandes",
        "Especias, chocolate amargo, frutos secos y acidez vinosa."
    ),
    GrainTip(
        "Colombia",
        "Origen",
        "Chocolate con leche, frutos rojos, caramelo y cuerpo redondo."
    ),
    GrainTip(
        "Etiopía",
        "Origen",
        "Cítrico, jazmín, bergamota, té negro y acidez viva."
    ),
    GrainTip(
        "Brasil",
        "Origen",
        "Cuerpo denso, acidez baja, avellana, cacao y nuez."
    )
)

private val extraRatioTips = drinkRatioTips.filter { tip ->
    prepareDrinks.none { it.waterParts == tip.water && it.milkParts == tip.milk }
}

@Composable
fun TipsScreen() {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Estilos", "Tiempos", "Granos")
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Tips y estilos", style = MaterialTheme.typography.headlineSmall)
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
    var drinkName by rememberSaveable { mutableStateOf(prepareDrinks.first().name) }
    val drink = prepareDrinks.firstOrNull { it.name == drinkName } ?: prepareDrinks.first()
    val amounts = drink.amounts(1.0)
    LazyColumn(
        modifier,
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "La extracción y la bebida son dos estilos distintos.",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "El ratio de extracción compara la dosis con el café de la taza. " +
                    "El agua y la leche se agregan después y no lo cambian.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item { Text("Extracción", style = MaterialTheme.typography.titleLarge) }
        items(extractionTips) { tip ->
            TipCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(tip.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        tip.range,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(tip.note, style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            Text("Bebidas", style = MaterialTheme.typography.titleLarge)
            Text(
                "Las mismas de Preparar un café, en tamaño 1×. " +
                    "El 1 de café : agua : leche es el café de la taza, no la dosis.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                prepareDrinks.forEach { option ->
                    FilterChip(
                        selected = option.name == drink.name,
                        onClick = { drinkName = option.name },
                        label = { Text(option.name) }
                    )
                }
            }
        }
        if (amounts != null) {
            item {
                val volume = amounts.output + amounts.water + amounts.milk
                TipCard {
                    Text(drink.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Café : agua : leche",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "1 : ${drink.waterParts.pretty()} : ${drink.milkParts.pretty()}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "${amounts.dose.pretty()} g de dosis · " +
                            "${amounts.output.pretty()} g de café · " +
                            "extracción 1:${drink.extractionRatio.pretty()}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    DrinkCupChart(
                        dose = amounts.dose,
                        output = amounts.output,
                        water = amounts.water,
                        milk = amounts.milk,
                        capacityCm3 = cupProfileFor(volume).capacityCm3,
                        milkFoam = amounts.milkFoam,
                        foamNote = amounts.milkFoam?.let { foamHowTo(drink.name, it) }
                    )
                }
            }
        }
        item {
            Text("Otras mezclas", style = MaterialTheme.typography.titleLarge)
            Text(
                "Sirven para reconocer un shot. En la app el agua y la leche se anotan en ml; " +
                    "acá 1 ml equivale a 1 g sobre 36 g de café.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(extraRatioTips) { recipe ->
            TipCard {
                Text(recipe.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "1 : ${recipe.water.pretty()} : ${recipe.milk.pretty()}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "${(36 * recipe.water).pretty()} ml de agua · " +
                        "${(36 * recipe.milk).pretty()} ml de leche",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            Text(
                "Macchiato también puede ir de 0,25 a 0,5 partes de leche. " +
                    "Latte macchiato es otra bebida. " +
                    "Agregar agua después de extraer no convierte un espresso en lungo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TimesTabContent(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier,
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Tiempo de extracción", style = MaterialTheme.typography.titleLarge)
            Text(
                "Se mide desde que entra el agua hasta cortar el flujo. " +
                    "Es una pista, no una regla: el sabor manda.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(timeTipsList) { tip ->
            TipCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(tip.timeRange, style = MaterialTheme.typography.titleMedium)
                    Text(
                        tip.trend,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    "Flujo: ${tip.flow}. Sabor: ${tip.taste}.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Qué probar: ${tip.advice}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun GrainsTabContent(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier,
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Granos", style = MaterialTheme.typography.titleLarge)
            Text(
                "Variedad y origen cambian el aroma, la acidez y el cuerpo. " +
                    "Son puntos de partida, no una ficha del café que compraste.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(grainTipsList) { tip ->
            TipCard {
                Text(tip.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    tip.type,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(tip.profile, style = MaterialTheme.typography.bodyMedium)
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
