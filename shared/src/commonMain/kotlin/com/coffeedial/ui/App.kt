package com.coffeedial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.coffeedial.auth.AuthRepository
import com.coffeedial.auth.AuthState
import com.coffeedial.auth.User
import com.coffeedial.backup.BackupFiles
import com.coffeedial.data.ShotRepository
import com.coffeedial.domain.Bean
import com.coffeedial.domain.Cup
import com.coffeedial.domain.Machine
import com.coffeedial.domain.Shot
import com.coffeedial.domain.ShotDraft
import com.coffeedial.sync.SyncEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

private val DraftSaver = Saver<ShotDraft, List<String>>(
    save = {
        listOf(
            it.beanName, it.roaster, it.dose, it.output, it.seconds,
            it.grind, it.temperature, it.milk, it.machine, it.notes, it.rating.toString(),
            it.extraWater, it.style, it.cup
        )
    },
    restore = {
        ShotDraft(
            it[0], it[1], it[2], it[3], it[4], it[5],
            it[6], it[7], it[8], it[9], it[10].toInt(),
            it.getOrElse(11) { "" }, it.getOrElse(12) { "" }, it.getOrElse(13) { "" }
        )
    }
)

@Composable
fun App(
    repository: ShotRepository,
    backupFiles: BackupFiles,
    authRepository: AuthRepository,
    localWeb: Boolean = false
) {
    var screen by rememberSaveable { mutableStateOf("home") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var draft by rememberSaveable(stateSaver = DraftSaver) { mutableStateOf(ShotDraft()) }
    var shots by remember { mutableStateOf<List<Shot>?>(null) }
    var machines by remember { mutableStateOf<List<Machine>>(emptyList()) }
    var beansList by remember { mutableStateOf<List<Bean>>(emptyList()) }
    var cupsList by remember { mutableStateOf<List<Cup>>(emptyList()) }
    var loadError by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var selectedStyle by rememberSaveable { mutableStateOf<String?>(null) }
    val styleOptions = shots.orEmpty().map { it.style.orEmpty().trim() }
        .distinctBy { it.lowercase() }.sortedBy { it.lowercase() }
    LaunchedEffect(styleOptions) {
        if (selectedStyle != null &&
            styleOptions.none { it.lowercase() == selectedStyle }
        ) {
            selectedStyle = null
        }
    }
    val filteredShots = shots.orEmpty().filter {
        selectedStyle == null || it.style.orEmpty().trim().lowercase() == selectedStyle
    }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val syncEngine = remember(repository, authRepository) { SyncEngine(repository, authRepository) }
    val authState by authRepository.state.collectAsState()
    LaunchedEffect(authState) {
        if (authState is AuthState.LoggedIn) syncEngine.performSync()
    }
    PlatformBack(enabled = screen != "home") {
        if (!saving && !backupFiles.busy) {
            screen = when (screen) {
                "admin" -> "account"

                "detail" -> "history"

                "edit" -> "detail"

                "machines", "beans", "cups",
                "account", "backup", "analysis" -> "home"

                else -> "home"
            }
        }
    }
    LaunchedEffect(repository, retry) {
        loadError = false
        try {
            launch { repository.history.collect { shots = it } }
            launch { repository.machines.collect { machines = it } }
            launch { repository.beans.collect { beansList = it } }
            launch { repository.cups.collect { cupsList = it } }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            loadError = true
        }
    }
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF704A32),
            background = Color(0xFFFCF8F3),
            surface = Color(0xFFFCF8F3),
            secondaryContainer = Color(0xFFEDE0CF)
        )
    ) {
        Scaffold { padding ->
            Column(
                Modifier.widthIn(
                    max = 960.dp
                ).fillMaxSize().padding(padding).padding(horizontal = 20.dp).imePadding()
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Coffee Dial",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (screen == "home") {
                            TextButton(onClick = { screen = "account" }) {
                                Text("Cuenta")
                            }
                        } else if (screen != "home") {
                            TextButton(enabled = !saving && !backupFiles.busy, onClick = {
                                screen = when (screen) {
                                    "admin" -> "account"

                                    "detail" -> "history"

                                    "edit" -> "detail"

                                    "machines", "beans", "cups", "tips",
                                    "account", "backup", "analysis" -> "home"

                                    else -> "home"
                                }
                            }) {
                                Text("Volver")
                            }
                        }
                    }
                }
                when (screen) {
                    "backup" -> BackupScreen(repository, backupFiles, saving, { saving = it })

                    "account" -> AccountScreen(
                        authRepository = authRepository,
                        syncEngine = syncEngine,
                        onAdmin = { screen = "admin" }
                    )

                    "admin" -> AdminScreen(authRepository)

                    "analysis" -> AnalysisScreen(shots.orEmpty())

                    "tips" -> TipsScreen()

                    "machines" -> MachinesScreen(
                        machines = machines,
                        saving = saving,
                        saveError = saveError,
                        onSaveMachine = { machineId, machineDraft, onSaved ->
                            saving = true
                            saveError = null
                            scope.launch {
                                try {
                                    if (machineId == null) {
                                        repository.saveMachine(machineDraft)
                                    } else {
                                        repository.updateMachine(machineId, machineDraft)
                                    }
                                    onSaved()
                                    scope.launch { syncEngine.performSync() }
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (_: Exception) {
                                    saveError = "No pudimos guardar la máquina."
                                } finally {
                                    saving = false
                                }
                            }
                        },
                        onDeleteMachine = { machineId ->
                            saving = true
                            scope.launch {
                                try {
                                    repository.deleteMachine(machineId)
                                    scope.launch { syncEngine.performSync() }
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (_: Exception) {
                                } finally {
                                    saving = false
                                }
                            }
                        }
                    )

                    "beans" -> BeansScreen(
                        beans = beansList,
                        saving = saving,
                        saveError = saveError,
                        onSaveBean = { beanId, beanDraft, onSaved ->
                            saving = true
                            saveError = null
                            scope.launch {
                                try {
                                    if (beanId == null) {
                                        repository.saveBean(beanDraft)
                                    } else {
                                        repository.updateBean(beanId, beanDraft)
                                    }
                                    onSaved()
                                    scope.launch { syncEngine.performSync() }
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (_: Exception) {
                                    saveError = "No pudimos guardar el café."
                                } finally {
                                    saving = false
                                }
                            }
                        },
                        onDeleteBean = { beanId ->
                            saving = true
                            scope.launch {
                                try {
                                    repository.deleteBean(beanId)
                                    scope.launch { syncEngine.performSync() }
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (_: Exception) {
                                } finally {
                                    saving = false
                                }
                            }
                        }
                    )

                    "cups" -> CupsScreen(
                        cups = cupsList,
                        saving = saving,
                        saveError = saveError,
                        onSaveCup = { cupId, cupDraft, onSaved ->
                            saving = true
                            saveError = null
                            scope.launch {
                                try {
                                    if (cupId == null) {
                                        repository.saveCup(cupDraft)
                                    } else {
                                        repository.updateCup(cupId, cupDraft)
                                    }
                                    onSaved()
                                    scope.launch { syncEngine.performSync() }
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (_: Exception) {
                                    saveError = "No pudimos guardar la taza."
                                } finally {
                                    saving = false
                                }
                            }
                        },
                        onDeleteCup = { cupId ->
                            saving = true
                            scope.launch {
                                try {
                                    repository.deleteCup(cupId)
                                    scope.launch { syncEngine.performSync() }
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (_: Exception) {
                                } finally {
                                    saving = false
                                }
                            }
                        }
                    )

                    "new", "edit" -> ShotForm(
                        draft = draft,
                        machines = machines,
                        beans = beansList,
                        cups = cupsList,
                        onChange = {
                            draft = it
                            saveError = null
                        },
                        saving = saving,
                        saveError = saveError,
                        isEditing = screen == "edit",
                        onNavigateToMachines = { screen = "machines" },
                        onNavigateToBeans = { screen = "beans" },
                        onNavigateToCups = { screen = "cups" },
                        onSave = {
                            saving = true
                            saveError = null
                            scope.launch {
                                try {
                                    if (screen == "edit" && selectedId != null) {
                                        repository.update(selectedId!!, draft)
                                        screen = "detail"
                                    } else {
                                        repository.save(draft)
                                        screen = "history"
                                    }
                                    draft = ShotDraft()
                                    scope.launch { syncEngine.performSync() }
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (_: Exception) {
                                    saveError =
                                        "No pudimos guardar el shot. Tus datos siguen acá; " +
                                        "intentá otra vez."
                                } finally {
                                    saving = false
                                }
                            }
                        }
                    )

                    "detail" -> {
                        val shot = shots.orEmpty().find { it.id == selectedId }
                        if (shot == null) {
                            Text("No encontramos este shot.")
                        } else {
                            ShotDetail(
                                shot = shot,
                                onEdit = {
                                    draft = ShotDraft(
                                        beanName = shot.bean.name,
                                        roaster = shot.bean.roaster,
                                        dose = shot.dose.pretty(),
                                        output = shot.output.pretty(),
                                        seconds = shot.seconds.pretty(),
                                        grind = shot.grind,
                                        temperature = shot.temperature?.pretty() ?: "",
                                        milk = shot.milk?.pretty() ?: "",
                                        extraWater = shot.extraWater?.pretty() ?: "",
                                        style = shot.style.orEmpty(),
                                        machine = shot.machine ?: "",
                                        notes = shot.notes,
                                        rating = shot.rating,
                                        cup = shot.cup ?: ""
                                    )
                                    screen = "edit"
                                },
                                onDelete = {
                                    saving = true
                                    scope.launch {
                                        try {
                                            repository.delete(shot.id)
                                            screen = "history"
                                            scope.launch { syncEngine.performSync() }
                                        } catch (cancelled: CancellationException) {
                                            throw cancelled
                                        } catch (_: Exception) {
                                        } finally {
                                            saving = false
                                        }
                                    }
                                },
                                saving = saving
                            )
                        }
                    }

                    else -> {
                        val userName = (authState as? AuthState.LoggedIn)?.user?.displayName
                            ?.trim()?.takeIf { it.isNotEmpty() }?.substringBefore(' ')

                        HomeScreen(
                            screen = screen,
                            shots = shots,
                            filteredShots = filteredShots,
                            selectedStyle = selectedStyle,
                            styleOptions = styleOptions,
                            loadError = loadError,
                            userName = userName,
                            onSelectStyle = { selectedStyle = it },
                            onNewShot = { screen = "new" },
                            onSelectShot = {
                                selectedId = it.id
                                screen = "detail"
                            },
                            onNavigateToHistory = { screen = "history" },
                            onNavigateToBeans = { screen = "beans" },
                            onNavigateToMachines = { screen = "machines" },
                            onNavigateToCups = { screen = "cups" },
                            onNavigateToTips = { screen = "tips" },
                            onNavigateToBackup = { screen = "backup" },
                            onNavigateToAnalysis = { screen = "analysis" },
                            onRetry = { retry++ }
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// COMPOSE PREVIEWS
// -----------------------------------------------------------------------------

@Preview(name = "App Principal - Interactivo", showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun AppInteractivePreview() {
    val store = remember {
        object : com.coffeedial.data.SnapshotStore {
            var data: String? = null
            override suspend fun read(): String? = data
            override suspend fun compareAndSet(expected: String?, next: String): Boolean {
                data = next
                return true
            }
        }
    }
    var repository by remember { mutableStateOf<com.coffeedial.data.SnapshotRepository?>(null) }
    LaunchedEffect(Unit) {
        val repo = com.coffeedial.data.SnapshotRepository.open(store)
        repo.save(mockDraft)
        repository = repo
    }

    MaterialTheme {
        if (repository != null) {
            App(
                repository = repository!!,
                backupFiles = remember { BackupFiles() },
                authRepository = remember {
                    com.coffeedial.auth.InMemoryAuthRepository(
                        AuthState.LoggedIn(
                            User(
                                id = "1",
                                email = "coffee@example.com",
                                displayName = "Agustin Sgarlata",
                                provider = com.coffeedial.auth.AuthProvider.GOOGLE
                            )
                        )
                    )
                }
            )
        }
    }
}
