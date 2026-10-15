package com.coffeedial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeedial.domain.PreparedAmounts
import com.coffeedial.domain.Shot
import com.coffeedial.ui.CoffeeButton as Button
import com.coffeedial.ui.CoffeeTextButton as TextButton
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
    onRepeatShot: ((Shot) -> Unit)? = null,
    onOpenTimer: () -> Unit = {},
    onOpenDialIn: () -> Unit = {},
    onRecipes: () -> Unit = {},
    onSelectShot: (Shot) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToBeans: () -> Unit,
    onNavigateToMachines: () -> Unit,
    onNavigateToCups: () -> Unit,
    onNavigateToTips: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToAnalysis: () -> Unit,
    onRetry: () -> Unit,
    refreshError: String? = null,
    onPrepare: (PreparedAmounts) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    when {
        loadError -> {
            Column(
                modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(strings.couldNotReadHistory)
                Button(onClick = onRetry) { Text(strings.actionRetry) }
            }
        }

        shots == null -> {
            Column(
                modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        }

        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = LocalNavigationSpace.current),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    if (screen == "home" && !userName.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                strings.greeting(userName),
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
                            strings.navBrew
                        } else {
                            strings.historyHeader(shots.size)
                        },
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        strings.shotsSavedOnDevice(shots.size),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    if (screen == "home" && !refreshError.isNullOrBlank()) {
                        Text(
                            strings.userMessage(refreshError),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    if (screen != "home") {
                        Button(onClick = onNewShot, modifier = Modifier.fillMaxWidth()) {
                            Text(strings.logAShot)
                        }
                    }
                }
                if (screen == "home") {
                    item {
                        CoffeeSection {
                            Text(
                                strings.nextExtraction,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                strings.logShotAndFlavor,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = onNewShot, modifier = Modifier.fillMaxWidth()) {
                                Text(strings.logAShot)
                            }
                        }
                    }
                    item {
                        CoffeeSection { PrepareCoffeeSection(onPrepare) }
                    }
                    item {
                        CoffeeSection {
                            Text(strings.yourWay, style = MaterialTheme.typography.titleLarge)
                            Text(
                                strings.repeatOrNew,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            CoffeeOutlinedButton(
                                onClick = onRecipes,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(strings.myRecipesRepeat)
                            }
                            CoffeeOutlinedButton(
                                onClick = onOpenTimer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(strings.startShotTimer)
                            }
                            TextButton(onClick = onNavigateToTips) {
                                Text(strings.ratiosAndStyles)
                            }
                        }
                    }
                    item {
                        Column(
                            Modifier.padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(strings.homeRecentShots, style = MaterialTheme.typography.titleLarge)
                            TextButton(onClick = onNavigateToHistory) {
                                Text(strings.viewHistoryCount(shots.size))
                            }
                        }
                    }
                }

                if (screen == "history" && shots.isNotEmpty()) {
                    item {
                        Text(strings.styleLabel, style = MaterialTheme.typography.titleMedium)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedStyle == null,
                                onClick = { onSelectStyle(null) },
                                label = { Text(strings.filterAll) }
                            )
                            styleOptions.forEach { option ->
                                FilterChip(
                                    selected = selectedStyle == option.lowercase(),
                                    onClick = { onSelectStyle(option.lowercase()) },
                                    label = {
                                        Text(
                                            if (option.isBlank()) {
                                                strings.filterNoStyle
                                            } else {
                                                strings.knownDrinkName(option)
                                            }
                                        )
                                    }
                                )
                            }
                        }
                        Text(strings.shotsFilterCount(filteredShots.size, shots.size))
                    }
                    if (filteredShots.isEmpty()) {
                        item {
                            Text(strings.noShotsWithStyle)
                        }
                    }
                }

                if (shots.isEmpty()) {
                    item {
                        Text(
                            strings.homeNoShotsYet
                        )
                    }
                }

                items(
                    if (screen == "home") shots.take(3) else filteredShots,
                    key = { it.id }
                ) { shot ->
                    ShotCard(shot, onRepeat = onRepeatShot?.let { repeat -> { repeat(shot) } }) {
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
