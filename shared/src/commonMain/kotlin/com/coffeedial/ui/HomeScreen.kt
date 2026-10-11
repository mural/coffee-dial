package com.coffeedial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeedial.domain.Shot
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun HomeScreen(
    screen: String = "home",
    shots: List<Shot>?,
    filteredShots: List<Shot>,
    selectedStyle: String?,
    styleOptions: List<String>,
    loadError: Boolean,
    userName: String? = null,
    onSelectStyle: (String?) -> Unit,
    onNewShot: () -> Unit,
    onOpenTimer: () -> Unit = {},
    onOpenDialIn: () -> Unit = {},
    onSelectShot: (Shot) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToBeans: () -> Unit,
    onNavigateToMachines: () -> Unit,
    onNavigateToCups: () -> Unit,
    onNavigateToTips: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToAnalysis: () -> Unit,
    onRetry: () -> Unit
) {
    when {
        loadError -> {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("No pudimos leer tu historial.")
                Button(onClick = onRetry) { Text("Reintentar") }
            }
        }

        shots == null -> {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        }

        else -> {
            LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = LocalNavigationSpace.current), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    if (screen == "home" && !userName.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "¡Hola, $userName!",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Icon(
                                CoffeeCup,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(
                        if (screen == "home") {
                            "Preparar"
                        } else {
                            "Historial (${shots.size})"
                        },
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        "${shots.size} shots · guardados en este dispositivo",
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    Button(onClick = onNewShot, modifier = Modifier.fillMaxWidth()) {
                        Text("Registrar un shot")
                    }
                    if (screen == "home") {
                        Text("Timer y recetas para tu próxima extracción.")
                        Button(onClick = onOpenTimer, modifier = Modifier.fillMaxWidth()) {
                            Text("Iniciar Shot Timer")
                        }
                        TextButton(onClick = onNavigateToTips) { Text("Ratios y estilos de café") }
                        Text(
                            "Últimos shots",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                if (screen == "history" && shots.isNotEmpty()) {
                    item {
                        Text("Estilo", style = MaterialTheme.typography.titleMedium)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedStyle == null,
                                onClick = { onSelectStyle(null) },
                                label = { Text("Todos") }
                            )
                            styleOptions.forEach { option ->
                                FilterChip(
                                    selected = selectedStyle == option.lowercase(),
                                    onClick = { onSelectStyle(option.lowercase()) },
                                    label = { Text(option.ifBlank { "Sin estilo" }) }
                                )
                            }
                        }
                        Text("${filteredShots.size} de ${shots.size} shots")
                    }
                    if (filteredShots.isEmpty()) {
                        item {
                            Text("No hay shots con este estilo.")
                        }
                    }
                }

                if (shots.isEmpty()) {
                    item {
                        Text(
                            "Todavía no hay shots. Registrá tu receta y empezá a encontrar tu punto ideal."
                        )
                    }
                }

                items(
                    if (screen == "home") shots.take(3) else filteredShots,
                    key = { it.id }
                ) { shot ->
                    ShotCard(shot) {
                        onSelectShot(shot)
                    }
                }
            }
        }
    }
}

@Preview(
    name = "Pantalla Principal (HomeScreen) - Con Usuario",
    showBackground = true,
    widthDp = 400,
    heightDp = 800
)
@Composable
private fun HomeScreenWithUserPreview() {
    MaterialTheme {
        HomeScreen(
            screen = "home",
            shots = listOf(mockShot),
            filteredShots = listOf(mockShot),
            selectedStyle = null,
            styleOptions = listOf("Espresso", "Americano", "Latte"),
            loadError = false,
            userName = "Agustin",
            onSelectStyle = {},
            onNewShot = {},
            onSelectShot = {},
            onNavigateToHistory = {},
            onNavigateToBeans = {},
            onNavigateToMachines = {},
            onNavigateToCups = {},
            onNavigateToTips = {},
            onNavigateToBackup = {},
            onNavigateToAnalysis = {},
            onRetry = {}
        )
    }
}

@Preview(
    name = "Pantalla Historial (HomeScreen) - Lista Completa",
    showBackground = true,
    widthDp = 400,
    heightDp = 800
)
@Composable
private fun HomeScreenHistoryPreview() {
    MaterialTheme {
        HomeScreen(
            screen = "history",
            shots = listOf(mockShot),
            filteredShots = listOf(mockShot),
            selectedStyle = "espresso",
            styleOptions = listOf("Espresso", "Americano", "Latte"),
            loadError = false,
            userName = null,
            onSelectStyle = {},
            onNewShot = {},
            onSelectShot = {},
            onNavigateToHistory = {},
            onNavigateToBeans = {},
            onNavigateToMachines = {},
            onNavigateToCups = {},
            onNavigateToTips = {},
            onNavigateToBackup = {},
            onNavigateToAnalysis = {},
            onRetry = {}
        )
    }
}
