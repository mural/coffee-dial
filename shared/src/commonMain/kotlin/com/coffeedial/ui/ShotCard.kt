package com.coffeedial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeedial.domain.Bean
import com.coffeedial.domain.Shot
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider

internal class ShotPreviewParameterProvider : PreviewParameterProvider<Shot> {
    override val values: Sequence<Shot> = sequenceOf(
        mockShot,
        Shot(
            id = "2",
            bean = Bean(id = "2", name = "Colombia Excelso", roaster = "Puerto Blest"),
            dose = 18.0,
            output = 26.0,
            seconds = 28.0,
            grind = "14",
            temperature = 93.0,
            milk = null,
            extraWater = null,
            machine = "Breville Dual Boiler",
            rating = 5,
            notes = "Espresso clásico perfecto, excelente crema.",
            style = "Espresso",
            createdAt = 1791231400000L
        ),
        Shot(
            id = "3",
            bean = Bean(id = "3", name = "Ethiopia Yirgacheffe", roaster = "Café Fuego"),
            dose = 18.0,
            output = 45.0,
            seconds = 32.0,
            grind = "16",
            temperature = 91.0,
            milk = null,
            extraWater = 150.0,
            machine = "Sage Barista Express",
            rating = 4,
            notes = "Notas cítricas y florales.",
            style = "Americano",
            createdAt = 1791031400000L
        ),
        Shot(
            id = "4",
            bean = Bean(id = "4", name = "Brasil Mogiana", roaster = "Indumentaria Coffee"),
            dose = 19.0,
            output = 58.0,
            seconds = 25.0,
            grind = "11",
            temperature = 92.0,
            milk = 120.0,
            extraWater = null,
            machine = "DeLonghi Dedica",
            rating = 5,
            notes = "Latte cremoso con notas a chocolate y avellanas.",
            style = "Flat White",
            createdAt = 1791031400000L
        )
    )
}

@Composable
internal fun ShotCard(shot: Shot, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(shot.bean.name, style = MaterialTheme.typography.titleLarge)
                val details = listOfNotNull(
                    shot.style?.takeIf { it.isNotBlank() },
                    shot.cup?.takeIf { it.isNotBlank() }?.let { "Taza: $it" }
                )
                if (details.isNotEmpty()) {
                    Text(
                        details.joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ShotMetric(BeanIcon, "Dosis", "${shot.dose.pretty()} g", Modifier.weight(1f))
                ShotMetric(CoffeeCup, "Output", "${shot.output.pretty()} g", Modifier.weight(1f))
            }
            if (shot.milk != null || shot.extraWater != null) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (shot.milk != null) {
                        ShotMetric(
                            MilkIcon,
                            "Leche",
                            "${shot.milk.pretty()} ml",
                            Modifier.weight(1f)
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    if (shot.extraWater != null) {
                        ShotMetric(
                            WaterIcon,
                            "Agua extra",
                            "${shot.extraWater.pretty()} ml",
                            Modifier.weight(1f)
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                val total = shot.output + (shot.milk ?: 0.0) + (shot.extraWater ?: 0.0)
                ShotMetric(MugIcon, "Total", "≈ ${total.pretty()} ml", Modifier.weight(1f))
                ShotMetric(TimerIcon, "Tiempo", "${shot.seconds.pretty()} s", Modifier.weight(1f))
            }

            val ratioCategory = when {
                shot.ratio <= 1.5 -> "ristretto"
                shot.ratio <= 2.5 -> "espresso"
                shot.ratio <= 3.0 -> "espresso largo"
                else -> "lungo"
            }

            Text(
                buildAnnotatedString {
                    append("Ratio 1:")
                    withStyle(
                        SpanStyle(
                            fontSize = if (shot.ratio < 3) 20.sp else 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(shot.ratio.pretty())
                    }
                    append(" -> $ratioCategory")
                },
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                timestamp(shot.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            CoffeeRating(shot.rating)
        }
    }
}

@Composable
private fun ShotMetric(icon: ImageVector, label: String, value: String, modifier: Modifier) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Preview(name = "Tarjeta de Shot", showBackground = true)
@Composable
private fun ShotCardPreview(@PreviewParameter(ShotPreviewParameterProvider::class) shot: Shot) {
    MaterialTheme {
        ShotCard(
            shot = shot,
            onClick = {}
        )
    }
}
