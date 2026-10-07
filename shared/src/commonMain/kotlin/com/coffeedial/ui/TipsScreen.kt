package com.coffeedial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
        "🔵 Muy subextraído",
        "Aguado, ácido/agrio, poco cuerpo",
        "Molienda mucho más fina"
    ),
    TimeTip(
        "15-20 s",
        "Rápido",
        "🔵 Subextraído",
        "Ácido, poco dulce, final corto",
        "Molienda más fina"
    ),
    TimeTip(
        "20-25 s",
        "Algo rápido",
        "🟡 Puede estar subextraído",
        "Ya puede ser rico, depende del grano",
        "Algo más fino si le falta cuerpo"
    ),
    TimeTip(
        "25-30 s",
        "Normal",
        "🟢 Zona clásica",
        "Balance, dulzor, cuerpo perfecto",
        "Ajustar fino según el gusto"
    ),
    TimeTip(
        "30-35 s",
        "Lento",
        "🟢/🟡 Puede estar perfecto",
        "Más cuerpo e intensidad",
        "Si está rico, no tocar nada"
    ),
    TimeTip(
        "35-40 s",
        "Muy lento",
        "🟠 Tendencia a sobreextracción",
        "Amargo, seco / astringente",
        "Molienda algo más gruesa"
    ),
    TimeTip(
        ">40-45 s",
        "Goteo / restricción",
        "🔴 Probable sobreextracción",
        "Muy amargo, seco, áspero",
        "Molienda más gruesa"
    )
)

private data class StyleTip(
    val title: String,
    val range: String,
    val example: String,
    val description: String
)

private val styleTipsList = listOf(
    StyleTip(
        "Ristretto",
        "Ratio 1:1 a 1:1,5",
        "Ratio 1:1.2 -> ristretto",
        "Shot corto y concentrado. Sabor intenso, cuerpo espeso, dulzor marcado y acidez limpia con amargor mínimo."
    ),
    StyleTip(
        "Espresso",
        "Ratio 1:1,5 a 1:2,5",
        "Ratio 1:2 -> espresso",
        "La zona dorada estándar. Balance perfecto entre cuerpo, crema avellanada, acidez aromática y notas dulces."
    ),
    StyleTip(
        "Espresso largo",
        "Ratio 1:2,5 a 1:3",
        "Ratio 1:2.8 -> espresso largo",
        "Extracción extendida que resalta notas florales y frutales con menor densidad de crema y cuerpo más fluido."
    ),
    StyleTip(
        "Lungo",
        "Ratio mayor a 1:3",
        "Ratio 1:3.5 -> lungo",
        "Volumen amplio obtenido pasando más agua. Trae sabores más estirados y ligeros, ideal para bebidas compuestas."
    )
)

private data class GrainTip(val name: String, val type: String, val profile: String)

private val grainTipsList = listOf(
    GrainTip(
        "Arábica vs. Robusta",
        "Especies principales",
        "Arábica: Complejidad aromática, acidez refinada y dulzor (cafeína 1.2%). Robusta: Alto cuerpo, crema espesa, amargor persistente (cafeína 2.2%)."
    ),
    GrainTip(
        "Bourbon / Caturra / Catuaí",
        "Varietales clásicos de especialidad",
        "Cuerpo sedoso, notas chocolatadas, caramelo, frutas rojas y acidez cítrica muy balanceada."
    ),
    GrainTip(
        "Geisha / Gesha",
        "Varietal exótico de alta gama",
        "Aroma floral penetrante (Jazmín), acidez cítrica brillante (Bergamota), notas a lima y cuerpo ligero estilo té."
    ),
    GrainTip(
        "Pacamara / Maragogipe",
        "Granos de tamaño gigante",
        "Notas complejas a especias, chocolate amargo, frutos secos y acidez vinosa profunda."
    ),
    GrainTip(
        "Origen Colombia",
        "Perfil de origen",
        "Notas a chocolate con leche, frutos rojos, caramelo de caña y cuerpo redondo súper versátil."
    ),
    GrainTip(
        "Origen Etiopía",
        "Cuna del café",
        "Perfil cítrico, notas a jazmín, bergamota, té negro y frutas de hueso con acidez viva."
    ),
    GrainTip(
        "Origen Brasil",
        "Perfil de origen",
        "Cuerpo denso, acidez baja, notas avellanadas, manteca de cacao y dulzor de nuez."
    )
)

@Composable
fun TipsScreen() {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Estilos", "Tiempos", "Granos")

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Guía y Consejos de Extracción", style = MaterialTheme.typography.headlineSmall)

        PrimaryTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            fontWeight = if (selectedTab ==
                                index
                            ) {
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
            0 -> StylesTabContent()
            1 -> TimesTabContent()
            2 -> GrainsTabContent()
        }
    }
}

@Composable
private fun StylesTabContent() {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(
                "Guía de Estilos y Ratios de Extracción",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "El ratio es la proporción entre gramos de café molido (dosis) y gramos de bebida obtenida (output).",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        }
        items(styleTipsList) { tip ->
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            tip.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            tip.range,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        "Ejemplo: ${tip.example}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(tip.description, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun TimesTabContent() {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(
                "Tiempos de Extracción y Solución de Problemas",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "El tiempo aproximado de extracción se mide desde que la bomba activa el agua hasta detener el flujo.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        }
        items(timeTipsList) { tip ->
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Tiempo: ${tip.timeRange}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(tip.trend, style = MaterialTheme.typography.labelMedium)
                    }
                    Text(
                        "Flujo: ${tip.flow} · Sabor: ${tip.taste}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Qué probar: ${tip.advice}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun GrainsTabContent() {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(
                "Varietales, Orígenes y Perfiles de Sabor",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Cada variedad de grano y región aporta notas aromáticas, acidez y cuerpo característicos.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        }
        items(grainTipsList) { tip ->
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        tip.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
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
}

@Preview(showBackground = true)
@Composable
private fun TipsScreenPreview() {
    MaterialTheme {
        TipsScreen()
    }
}
