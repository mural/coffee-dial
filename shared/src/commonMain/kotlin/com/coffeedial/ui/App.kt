package com.coffeedial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.coffeedial.auth.AuthRepository
import com.coffeedial.auth.AuthState
import com.coffeedial.auth.User
import com.coffeedial.backup.BackupFiles
import com.coffeedial.data.ShotRepository
import com.coffeedial.domain.Bean
import com.coffeedial.domain.Cup
import com.coffeedial.domain.DialInContext
import com.coffeedial.domain.Grinder
import com.coffeedial.domain.Machine
import com.coffeedial.domain.Shot
import com.coffeedial.domain.ShotDraft
import com.coffeedial.domain.TasteProfile
import com.coffeedial.domain.toDraft
import com.coffeedial.sync.SyncEngine
import com.coffeedial.sync.SyncState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.compose.ui.tooling.preview.Preview

private val DraftSaver = Saver<ShotDraft, List<String>>(
    save = {
        listOf(
            it.beanName, it.roaster, it.dose, it.output, it.seconds,
            it.grind, it.temperature, it.milk, it.machine, it.notes, it.rating.toString(),
            it.extraWater, it.style, it.cup, it.firstDrop,
            it.taste?.let { value -> Json.encodeToString(value) }.orEmpty(),
            it.dialIn?.let { value -> Json.encodeToString(value) }.orEmpty(),
            it.machineId.orEmpty(),
            it.cupId.orEmpty(),
            it.grinder,
            it.grinderId.orEmpty()
        )
    },
    restore = {
        ShotDraft(
            it[0], it[1], it[2], it[3], it[4], it[5],
            it[6], it[7], it[8], it[9], it[10].toInt(),
            it.getOrElse(11) {
                ""
            },
            it.getOrElse(12) { "" }, it.getOrElse(13) { "" }, it.getOrElse(14) { "" },
            it.getOrNull(15)?.takeIf { value -> value.isNotEmpty() }?.let { value ->
                Json.decodeFromString<TasteProfile>(value)
            },
            it.getOrNull(16)?.takeIf { value -> value.isNotEmpty() }?.let { value ->
                Json.decodeFromString<com.coffeedial.domain.DialInContext>(value)
            },
            it.getOrNull(17)?.takeIf { value -> value.isNotEmpty() },
            it.getOrNull(18)?.takeIf { value -> value.isNotEmpty() },
            grinder = it.getOrElse(19) { "" },
            grinderId = it.getOrNull(20)?.takeIf { value -> value.isNotEmpty() }
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
    val timer = rememberSaveable(
        saver = Saver<com.coffeedial.domain.ShotTimer, List<Long>>(
            save = { listOf(it.elapsedMillis, it.firstDropMillis ?: -1L) },
            restore = {
                com.coffeedial.domain.ShotTimer(it[0], it[1].takeIf { value -> value >= 0 })
            }
        )
    ) { com.coffeedial.domain.ShotTimer() }
    var assistantOrigin by rememberSaveable { mutableStateOf("home") }
    var timerOrigin by rememberSaveable { mutableStateOf("home") }
    var pillar by rememberSaveable { mutableStateOf("home") }
    var screen by rememberSaveable { mutableStateOf("home") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var draft by rememberSaveable(stateSaver = DraftSaver) { mutableStateOf(ShotDraft()) }
    var shots by remember { mutableStateOf<List<Shot>?>(null) }
    var machines by remember { mutableStateOf<List<Machine>>(emptyList()) }
    var beansList by remember { mutableStateOf<List<Bean>>(emptyList()) }
    var cupsList by remember { mutableStateOf<List<Cup>>(emptyList()) }
    var grinders by remember { mutableStateOf<List<Grinder>>(emptyList()) }
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
    var refreshing by remember { mutableStateOf(false) }
    var refreshError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val syncEngine = remember(repository, authRepository) { SyncEngine(repository, authRepository) }
    val authState by authRepository.state.collectAsState()
    val syncState by syncEngine.state.collectAsState()
    LaunchedEffect(authState) {
        // Keep the notice hidden while restoring or verifying the session.
        updateWebNotice(authState is AuthState.LoggedIn || authState is AuthState.Authenticating)
        if (authState is AuthState.LoggedIn) syncEngine.performSync()
    }
    fun favorite(shot: Shot, context: DialInContext = shot.dialIn ?: DialInContext()) {
        saving = true
        saveError = null
        scope.launch {
            try {
                repository.update(
                    shot.id,
                    shot.toDraft().copy(
                        dialIn = context.copy(
                            favorite = !(
                                shot.dialIn?.favorite
                                    ?: false
                                )
                        )
                    )
                )
                scope.launch { syncEngine.performSync() }
            } catch (
                cancelled: CancellationException
            ) {
                throw cancelled
            } catch (
                _: Exception
            ) {
                saveError = "No pudimos guardar el favorito. Reintentá."
            } finally {
                saving = false
            }
        }
    }
    val roots = setOf("home", "track", "improve", "account")
    fun goBack() {
        if (saving || backupFiles.busy) return
        screen = when (screen) {
            "timer" -> {
                timer.pause()
                timerOrigin
            }

            "assistant" -> assistantOrigin

            "admin", "backup", "conflicts" -> "account"

            "edit" -> "detail"

            "detail" -> "history"

            else -> if (screen in roots) "home" else pillar
        }
        if (screen in roots) pillar = screen
    }
    PlatformBack(enabled = screen != "home") { goBack() }
    LaunchedEffect(repository, retry) {
        loadError = false
        try {
            launch { repository.history.collect { shots = it } }
            launch { repository.machines.collect { machines = it } }
            launch { repository.beans.collect { beansList = it } }
            launch { repository.cups.collect { cupsList = it } }
            launch { repository.grinders.collect { grinders = it } }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            loadError = true
        }
    }
    fun refreshHome() {
        if (refreshing) return
        scope.launch {
            refreshing = true
            refreshError = null
            try {
                if (authState is AuthState.LoggedIn) {
                    val result = syncEngine.performSync()
                    if (result.isFailure) {
                        refreshError = if (syncEngine.state.value is SyncState.Conflict) {
                            "Hay cambios en conflicto. Resolvelos en Cuenta."
                        } else {
                            result.exceptionOrNull()?.message ?: "No se pudo sincronizar."
                        }
                    }
                } else {
                    retry++
                    delay(400)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } finally {
                refreshing = false
            }
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
        var navigationHeight by remember { mutableIntStateOf(0) }
        var headerHeight by remember(screen) { mutableIntStateOf(0) }
        var headerOffset by remember(screen) { mutableStateOf(0f) }
        val headerScroll = remember(screen) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (available.y >= 0f) return Offset.Zero
                    val before = headerOffset
                    headerOffset =
                        (headerOffset + available.y).coerceIn(-headerHeight.toFloat(), 0f)
                    return Offset(0f, headerOffset - before)
                }
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    if (available.y <= 0f) return Offset.Zero
                    val before = headerOffset
                    headerOffset =
                        (headerOffset + available.y).coerceIn(-headerHeight.toFloat(), 0f)
                    return Offset(0f, headerOffset - before)
                }
            }
        }
        val bottomSpace = with(LocalDensity.current) { navigationHeight.toDp() } + 12.dp
        Scaffold { padding ->
            Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
                CompositionLocalProvider(
                    LocalNavigationSpace provides if (screen in roots) bottomSpace else 0.dp
                ) {
                    OptionalPullRefresh(
                        enabled = screen == "home",
                        isRefreshing = refreshing,
                        onRefresh = ::refreshHome
                    ) {
                        Column(
                            Modifier.widthIn(max = 960.dp).fillMaxSize()
                                .padding(horizontal = 20.dp)
                                .padding(
                                    bottom = if (screen in
                                        roots
                                    ) {
                                        0.dp
                                    } else {
                                        padding.calculateBottomPadding()
                                    }
                                )
                                .imePadding().then(
                                    if (screen in
                                        roots
                                    ) {
                                        Modifier.nestedScroll(headerScroll)
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            Row(
                                Modifier.fillMaxWidth().clipToBounds().layout {
                                        measurable,
                                        constraints
                                    ->
                                    val placeable = measurable.measure(constraints)
                                    headerHeight = placeable.height
                                    val offset = if (screen in roots) headerOffset.toInt() else 0
                                    layout(
                                        placeable.width,
                                        (placeable.height + offset).coerceAtLeast(0)
                                    ) {
                                        placeable.placeRelative(0, offset)
                                    }
                                },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Coffee Dial",
                                    style = MaterialTheme.typography.headlineMedium,
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (screen !in roots) {
                                        TextButton(enabled = !saving && !backupFiles.busy, onClick = {
                                            goBack()
                                        }) {
                                            Text("Volver")
                                        }
                                    }
                                }
                            }
                            when (screen) {
                                "backup" -> BackupScreen(repository, backupFiles, saving, {
                                    saving = it
                                })

                                "track" -> PillarScreen(
                                    "Registrar",
                                    "Tu café, tu equipo y cada extracción.",
                                    listOf(
                                        "Registrar un shot" to { screen = "new" },
                                        "Historial (${shots.orEmpty().size})" to
                                            { screen = "history" },
                                        "Cafés" to { screen = "beans" },
                                        "Máquinas" to { screen = "machines" },
                                        "Molinillos" to { screen = "grinders" },
                                        "Tazas" to { screen = "cups" }
                                    )
                                )

                                "improve" -> PillarScreen(
                                    "Mejorar",
                                    "Compará resultados y encontrá tu próxima receta.",
                                    listOf(
                                        "Ajustar espresso" to {
                                            assistantOrigin = "improve"
                                            selectedId = shots.orEmpty().firstOrNull()?.id
                                            saveError = null
                                            screen = "assistant"
                                        },
                                        "Análisis de extracciones" to { screen = "analysis" },
                                        "Comparar intentos y favoritos" to { screen = "history" },
                                        "Tips y estilos" to { screen = "tips" }
                                    )
                                )

                                "account" -> AccountScreen(
                                    authRepository = authRepository,
                                    syncEngine = syncEngine,
                                    onAdmin = { screen = "admin" },
                                    onBackup = { screen = "backup" },
                                    onConflicts = { screen = "conflicts" }
                                )

                                "conflicts" -> {
                                    val conflict = syncState as? SyncState.Conflict
                                    if (conflict == null) {
                                        Text(
                                            if (syncState is SyncState.Syncing) {
                                                "Resolviendo el conflicto…"
                                            } else {
                                                "No hay conflictos pendientes."
                                            }
                                        )
                                    } else {
                                        ConflictScreen(
                                            conflict,
                                            saving || syncState is SyncState.Syncing
                                        ) { record, keepLocal ->
                                            saving = true
                                            saveError = null
                                            scope.launch {
                                                try {
                                                    syncEngine.resolveConflict(
                                                        conflict.remote,
                                                        record,
                                                        keepLocal
                                                    )
                                                    if (syncEngine.state.value !is SyncState.Conflict) {
                                                        screen = "account"
                                                    }
                                                } catch (cancelled: CancellationException) {
                                                    throw cancelled
                                                } catch (_: Exception) {
                                                    saveError =
                                                        "No pudimos resolver el conflicto. Tus datos siguen guardados."
                                                } finally {
                                                    saving = false
                                                }
                                            }
                                        }
                                        saveError?.let { Text(it) }
                                    }
                                }

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
                                                    repository.updateMachine(
                                                        machineId,
                                                        machineDraft
                                                    )
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
                                    photos = backupFiles.photos,
                                    canAddPhoto = authState is AuthState.LoggedIn,
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

                                "grinders" -> GrindersScreen(
                                    grinders = grinders,
                                    saving = saving,
                                    saveError = saveError,
                                    onSaveGrinder = { grinderId, grinderDraft, onSaved ->
                                        saving = true
                                        saveError = null
                                        scope.launch {
                                            try {
                                                if (grinderId == null) {
                                                    repository.saveGrinder(grinderDraft)
                                                } else {
                                                    repository.updateGrinder(
                                                        grinderId,
                                                        grinderDraft
                                                    )
                                                }
                                                onSaved()
                                                scope.launch { syncEngine.performSync() }
                                            } catch (cancelled: CancellationException) {
                                                throw cancelled
                                            } catch (_: Exception) {
                                                saveError = "No pudimos guardar el molinillo."
                                            } finally {
                                                saving = false
                                            }
                                        }
                                    },
                                    onDeleteGrinder = { grinderId ->
                                        saving = true
                                        scope.launch {
                                            try {
                                                repository.deleteGrinder(grinderId)
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

                                "assistant" -> {
                                    val source = shots.orEmpty().find { it.id == selectedId }
                                    if (source == null) {
                                        Column {
                                            Text(
                                                "Registrá un shot y evaluá su sabor para empezar a ajustar."
                                            )
                                            TextButton(onClick = {
                                                screen = "new"
                                            }) { Text("Registrar shot") }
                                        }
                                    } else {
                                        DialInScreen(
                                            source,
                                            shots.orEmpty(),
                                            saving,
                                            saveError,
                                            onEdit = {
                                                draft = source.toDraft()
                                                saveError = null
                                                screen = "edit"
                                            },
                                            onFavorite = { favorite(source, it) },
                                            onSelect = {
                                                selectedId = it.id
                                                saveError = null
                                            },
                                            onNext = { next, context ->
                                                saving = true
                                                saveError = null
                                                scope.launch {
                                                    try {
                                                        repository.update(
                                                            source.id,
                                                            source.toDraft().copy(dialIn = context)
                                                        )
                                                        draft = next
                                                        screen = "new"
                                                        scope.launch { syncEngine.performSync() }
                                                    } catch (
                                                        cancelled: CancellationException
                                                    ) {
                                                        throw cancelled
                                                    } catch (_: Exception) {
                                                        saveError =
                                                            "No pudimos guardar el contexto. Reintentá."
                                                    } finally {
                                                        saving = false
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }

                                "timer" -> ShotTimerScreen(timer, timerOrigin != "home") {
                                        seconds,
                                        firstDrop
                                    ->
                                    draft = draft.copy(seconds = seconds, firstDrop = firstDrop)
                                    saveError = null
                                    screen = if (timerOrigin == "edit") "edit" else "new"
                                }

                                "new", "edit" -> ShotForm(
                                    draft = draft,
                                    machines = machines,
                                    beans = beansList,
                                    cups = cupsList,
                                    grinders = grinders,
                                    onChange = {
                                        draft = it
                                        saveError = null
                                    },
                                    saving = saving,
                                    saveError = saveError,
                                    isEditing = screen == "edit",
                                    onOpenTimer = {
                                        timerOrigin = screen
                                        screen = "timer"
                                    },
                                    onNavigateToMachines = { screen = "machines" },
                                    onNavigateToBeans = { screen = "beans" },
                                    onNavigateToCups = { screen = "cups" },
                                    onNavigateToGrinders = { screen = "grinders" },
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
                                                saveError = "No pudimos guardar el shot. " +
                                                    "Tus datos siguen acá; intentá otra vez."
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
                                            onDialIn = {
                                                assistantOrigin = "detail"
                                                saveError = null
                                                screen =
                                                    "assistant"
                                            },
                                            onFavorite = { favorite(shot) },
                                            error = saveError,
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
                                                    cup = shot.cup ?: "",
                                                    machineId = shot.machineId,
                                                    cupId = shot.cupId,
                                                    grinder = shot.grinder
                                                        ?: shot.dialIn?.grinder.orEmpty(),
                                                    grinderId = shot.grinderId,
                                                    firstDrop = shot.firstDrop?.pretty().orEmpty(),
                                                    taste = shot.taste,
                                                    dialIn = shot.dialIn
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
                                        onOpenDialIn = {
                                            assistantOrigin = "home"
                                            selectedId =
                                                shots.orEmpty().firstOrNull()?.id
                                            saveError = null
                                            screen =
                                                "assistant"
                                        },
                                        onOpenTimer = {
                                            timerOrigin = "home"
                                            screen = "timer"
                                        },
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
                                        onRetry = { retry++ },
                                        refreshError = refreshError,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
                if (screen in roots) {
                    Box(
                        Modifier.align(Alignment.BottomCenter).onSizeChanged {
                            navigationHeight =
                                it.height
                        }
                    ) {
                        PillarNavigation(pillar, enabled = !saving && !backupFiles.busy) {
                            pillar = it
                            screen = it
                            saveError = null
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionalPullRefresh(
    enabled: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    content: @Composable () -> Unit
) {
    if (enabled) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            content()
        }
    } else {
        content()
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
