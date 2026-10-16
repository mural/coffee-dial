package com.coffeedial.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.coffeedial.auth.AuthProvider
import com.coffeedial.auth.AuthRepository
import com.coffeedial.auth.AuthState
import com.coffeedial.auth.InMemoryAuthRepository
import com.coffeedial.auth.User
import com.coffeedial.backup.BackupFiles
import com.coffeedial.data.ShotRepository
import com.coffeedial.domain.AppLanguage
import com.coffeedial.domain.Bean
import com.coffeedial.domain.BrewDefault
import com.coffeedial.domain.Cup
import com.coffeedial.domain.DialInContext
import com.coffeedial.domain.Grinder
import com.coffeedial.domain.Machine
import com.coffeedial.domain.Recipe
import com.coffeedial.domain.Shot
import com.coffeedial.domain.ShotDraft
import com.coffeedial.domain.ShotTimer
import com.coffeedial.domain.newShot
import com.coffeedial.domain.toDraft
import com.coffeedial.sync.SyncEngine
import com.coffeedial.sync.SyncState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun App(
    repository: ShotRepository,
    backupFiles: BackupFiles,
    authRepository: AuthRepository,
    localWeb: Boolean = false
) {
    val timer = rememberSaveable(
        saver = Saver<ShotTimer, List<Long>>(
            save = { listOf(it.elapsedMillis, it.firstDropMillis ?: -1L) },
            restore = { ShotTimer(it[0], it[1].takeIf { value -> value >= 0 }) }
        )
    ) { ShotTimer() }
    var assistantOrigin by rememberSaveable { mutableStateOf("home") }
    var timerOrigin by rememberSaveable { mutableStateOf("home") }
    var pillar by rememberSaveable { mutableStateOf("home") }
    var screen by rememberSaveable { mutableStateOf("home") }
    var recipeInitial by rememberSaveable { mutableStateOf<String?>(null) }
    var recipes by remember { mutableStateOf<List<Recipe>>(emptyList()) }
    var comparisonInitial by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var draft by rememberSaveable(stateSaver = DraftSaver) { mutableStateOf(ShotDraft()) }
    var shots by remember { mutableStateOf<List<Shot>?>(null) }
    var brewDefaults by remember { mutableStateOf<List<BrewDefault>>(emptyList()) }
    var machines by remember { mutableStateOf<List<Machine>>(emptyList()) }
    var beansList by remember { mutableStateOf<List<Bean>>(emptyList()) }
    var cupsList by remember { mutableStateOf<List<Cup>>(emptyList()) }
    var grinders by remember { mutableStateOf<List<Grinder>>(emptyList()) }
    var themeMode by remember { mutableStateOf(readThemeMode()) }
    var appLanguage by remember { mutableStateOf(readAppLanguage()) }
    val resolvedLanguage = appLanguage.resolve()
    val strings = remember(resolvedLanguage) { AppStrings.forLanguage(resolvedLanguage) }
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
    LaunchedEffect(resolvedLanguage) {
        applyWebShellLanguage(resolvedLanguage == AppLanguage.ENGLISH)
    }
    LaunchedEffect(authState) {
        updateWebNotice(authState is AuthState.LoggedIn || authState is AuthState.Authenticating)
        if (authState is AuthState.LoggedIn) syncEngine.performSync()
    }
    fun newShot() {
        draft = brewDefaults.newShot(beansList, machines, grinders, cupsList)
        selectedId = null
        timer.reset()
        saveError = null
        screen = "new"
    }
    fun chooseDefault(kind: String, id: String?) {
        saving = true
        saveError = null
        scope.launch {
            try {
                repository.setBrewDefault(BrewDefault(kind, id))
                scope.launch { syncEngine.performSync() }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                saveError = strings.couldNotSaveDefault
            } finally {
                saving = false
            }
        }
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
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                saveError = strings.couldNotSaveFavorite
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

            "compare" -> if (comparisonInitial == null) "improve" else "detail"

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
            launch { repository.brewDefaults.collect { brewDefaults = it } }
            launch { repository.recipes.collect { recipes = it } }
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
                            strings.conflictChanges
                        } else {
                            val raw = result.exceptionOrNull()?.message
                            if (raw.isNullOrBlank()) {
                                strings.couldNotSync
                            } else {
                                strings.userMessage(raw)
                            }
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
    val dark = themeMode.isDark(isSystemInDarkTheme())
    MaterialTheme(
        colorScheme = if (dark) coffeeDarkColorScheme() else coffeeLightColorScheme()
    ) {
        ThemeChrome(dark)
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
                    LocalStrings provides strings,
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
                                    bottom = if (screen in roots) {
                                        0.dp
                                    } else {
                                        padding.calculateBottomPadding()
                                    }
                                )
                                .imePadding().then(
                                    if (screen in roots) {
                                        Modifier.nestedScroll(headerScroll)
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            AppHeader(
                                screen = screen,
                                roots = roots,
                                saving = saving,
                                busy = backupFiles.busy,
                                headerOffset = headerOffset,
                                onHeightMeasured = { headerHeight = it },
                                onGoBack = ::goBack
                            )
                            AppScreenContent(
                                screen = screen,
                                shots = shots,
                                filteredShots = filteredShots,
                                selectedStyle = selectedStyle,
                                styleOptions = styleOptions,
                                loadError = loadError,
                                refreshError = refreshError,
                                saving = saving,
                                saveError = saveError,
                                draft = draft,
                                machines = machines,
                                beansList = beansList,
                                cupsList = cupsList,
                                grinders = grinders,
                                recipes = recipes,
                                brewDefaults = brewDefaults,
                                selectedId = selectedId,
                                recipeInitial = recipeInitial,
                                comparisonInitial = comparisonInitial,
                                timer = timer,
                                timerOrigin = timerOrigin,
                                assistantOrigin = assistantOrigin,
                                themeMode = themeMode,
                                appLanguage = appLanguage,
                                authState = authState,
                                syncState = syncState,
                                repository = repository,
                                backupFiles = backupFiles,
                                authRepository = authRepository,
                                syncEngine = syncEngine,
                                scope = scope,
                                onNavigate = {
                                    screen = it
                                    if (it in roots) pillar = it
                                },
                                onUpdateDraft = { draft = it },
                                onUpdateSelectedId = { selectedId = it },
                                onUpdateRecipeInitial = { recipeInitial = it },
                                onUpdateComparisonInitial = { comparisonInitial = it },
                                onUpdateAssistantOrigin = { assistantOrigin = it },
                                onUpdateTimerOrigin = { timerOrigin = it },
                                onUpdateThemeMode = {
                                    themeMode = it
                                    writeThemeMode(it)
                                },
                                onUpdateAppLanguage = {
                                    appLanguage = it
                                    writeAppLanguage(it)
                                },
                                onUpdateSaveError = { saveError = it },
                                onUpdateSaving = { saving = it },
                                onNewShot = ::newShot,
                                onFavorite = ::favorite,
                                onRetry = { retry++ },
                                onSelectStyle = { selectedStyle = it },
                                onChooseDefault = ::chooseDefault
                            )
                        }
                    }
                }
                if (screen in roots) {
                    Box(
                        Modifier.align(Alignment.BottomCenter).onSizeChanged {
                            navigationHeight = it.height
                        }
                    ) {
                        CompositionLocalProvider(LocalStrings provides strings) {
                            PillarNavigation(
                                pillar,
                                enabled = !saving && !backupFiles.busy
                            ) {
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
}

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
                    InMemoryAuthRepository(
                        AuthState.LoggedIn(
                            User(
                                id = "1",
                                email = "coffee@example.com",
                                displayName = "Agustin Sgarlata",
                                provider = AuthProvider.GOOGLE
                            )
                        )
                    )
                }
            )
        }
    }
}
