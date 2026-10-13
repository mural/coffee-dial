package com.coffeedial.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.coffeedial.ui.CoffeeTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeedial.domain.Shot
import com.coffeedial.domain.compareShots

@Composable
fun ShotComparisonScreen(
    shots: List<Shot>,
    initialId: String?,
    saving: Boolean,
    error: String?,
    onPrefer: (Shot) -> Unit
) {
    var selected by rememberSaveable(initialId) {
        val shot = shots.find { it.id == initialId }
        val parent = shots.find { it.id == shot?.dialIn?.previousShotId }
        mutableStateOf(listOfNotNull(parent?.id, shot?.id).distinct().joinToString("|"))
    }
    val ids = selected.split('|').filter { it.isNotEmpty() }
    val chosen = ids.mapNotNull { id -> shots.find { it.id == id } }
    var beanId by rememberSaveable { mutableStateOf<String?>(null) }
    var onlyFavorites by rememberSaveable { mutableStateOf(false) }
    val beans = shots.map { it.bean }.distinctBy { it.id }
    val candidates = shots.filter {
        (beanId == null || it.bean.id == beanId) &&
            (!onlyFavorites || it.dialIn?.favorite == true)
    }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text("Comparar intentos", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Elegí 2 o 3 shots. El primero será la referencia; los cambios aparecen resaltados."
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        if (chosen.isNotEmpty()) {
            item {
                Text(
                    "Seleccionados (${chosen.size}/3)",
                    style = MaterialTheme.typography.titleMedium
                )
                chosen.forEachIndexed { index, shot ->
                    Row {
                        TextButton(onClick = {
                            selected =
                                (
                                    listOf(shot.id) + chosen.filterNot {
                                        it.id == shot.id
                                    }.map { it.id }
                                    ).joinToString("|")
                        }, enabled = !saving) {
                            Text(
                                if (index ==
                                    0
                                ) {
                                    "Referencia: ${shot.bean.name}"
                                } else {
                                    "${index + 1}: usar como referencia"
                                }
                            )
                        }
                        TextButton(onClick = {
                            selected =
                                ids.filterNot { it == shot.id }.joinToString("|")
                        }, enabled = !saving) { Text("Quitar") }
                    }
                    Text(timestamp(shot.createdAt), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (chosen.size >= 2) {
            item {
                if (chosen.map { it.bean.id }.distinct().size > 1) {
                    Text(
                        "Son cafés distintos: las diferencias de sabor no dependen solo de los ajustes."
                    )
                }
                Text(
                    "Comparación descriptiva: tiempo y ratio no determinan por sí solos la calidad. Agua y leche también cambian el sabor.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Deslizá la tabla hacia los lados para ver todos los intentos.",
                    style = MaterialTheme.typography.bodySmall
                )
                Column(Modifier.horizontalScroll(rememberScrollState())) {
                    Row {
                        Text("", Modifier.width(116.dp))
                        chosen.forEachIndexed { index, shot ->
                            Column(
                                Modifier.width(172.dp).padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    if (index ==
                                        0
                                    ) {
                                        "Referencia"
                                    } else {
                                        "Intento ${index + 1}"
                                    },
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    timestamp(shot.createdAt),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                TextButton(
                                    onClick = { onPrefer(shot) },
                                    enabled =
                                        !saving && shot.dialIn?.favorite != true
                                ) {
                                    Text(
                                        if (shot.dialIn?.favorite ==
                                            true
                                        ) {
                                            "Favorito"
                                        } else {
                                            "Prefiero este"
                                        }
                                    )
                                }
                            }
                        }
                    }
                    compareShots(chosen).forEach { row ->
                        HorizontalDivider()
                        Row(Modifier.height(IntrinsicSize.Min)) {
                            Text(
                                row.label,
                                Modifier.width(116.dp).padding(8.dp),
                                style = MaterialTheme.typography.labelLarge
                            )
                            row.values.forEachIndexed { index, value ->
                                Column(
                                    Modifier.width(172.dp).fillMaxHeight()
                                        .background(
                                            if (row.changed(
                                                    index
                                                )
                                            ) {
                                                MaterialTheme.colorScheme.secondaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surface
                                            }
                                        )
                                        .padding(8.dp)
                                ) {
                                    Text(value)
                                    row.sensory?.get(index)?.let { amount ->
                                        Box(
                                            Modifier.padding(
                                                top = 6.dp
                                            ).fillMaxWidth().height(
                                                5.dp
                                            ).background(MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Box(
                                                Modifier.fillMaxWidth(
                                                    amount / 5f
                                                ).fillMaxHeight().background(
                                                    MaterialTheme.colorScheme.primary
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Text(
                    "“Prefiero este” lo guarda como favorito para futuras referencias del asistente; no modifica los demás favoritos.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        item {
            Text("Agregar shots", style = MaterialTheme.typography.titleMedium)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(beanId == null, { beanId = null }, label = { Text("Todos los cafés") })
                beans.forEach { bean ->
                    FilterChip(beanId == bean.id, { beanId = bean.id }, label = { Text(bean.name) })
                }
            }
            FilterChip(onlyFavorites, {
                onlyFavorites = !onlyFavorites
            }, label = { Text("Solo favoritos") })
        }
        if (candidates.isEmpty()) item { Text("No hay shots para estos filtros.") }
        items(candidates, key = { it.id }) { shot ->
            FilterChip(
                selected = shot.id in ids,
                onClick = {
                    selected = if (shot.id in ids) {
                        ids.filterNot { it == shot.id }.joinToString("|")
                    } else {
                        (chosen.map { it.id } + shot.id).joinToString("|")
                    }
                },
                enabled = !saving && (shot.id in ids || chosen.size < 3),
                label = {
                    Column(Modifier.padding(vertical = 6.dp)) {
                        Text(
                            "${shot.bean.name} · ${shot.dose.pretty()} -> ${shot.output.pretty()} g · ${shot.rating}/5"
                        )
                        Text(timestamp(shot.createdAt), style = MaterialTheme.typography.bodySmall)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
