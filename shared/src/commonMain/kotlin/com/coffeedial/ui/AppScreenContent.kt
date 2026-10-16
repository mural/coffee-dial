package com.coffeedial.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeedial.auth.AuthRepository
import com.coffeedial.auth.AuthState
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
import com.coffeedial.domain.ThemeMode
import com.coffeedial.domain.asRecipe
import com.coffeedial.domain.newShot
import com.coffeedial.domain.repeatDraft
import com.coffeedial.domain.toDraft
import com.coffeedial.sync.SyncEngine
import com.coffeedial.sync.SyncState
import com.coffeedial.ui.CoffeeTextButton as TextButton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Composable
internal fun AppScreenContent(
    screen: String,
    shots: List<Shot>?,
    filteredShots: List<Shot>,
    selectedStyle: String?,
    styleOptions: List<String>,
    loadError: Boolean,
    refreshError: String?,
    saving: Boolean,
    saveError: String?,
    draft: ShotDraft,
    machines: List<Machine>,
    beansList: List<Bean>,
    cupsList: List<Cup>,
    grinders: List<Grinder>,
    recipes: List<Recipe>,
    brewDefaults: List<BrewDefault>,
    selectedId: String?,
    recipeInitial: String?,
    comparisonInitial: String?,
    timer: ShotTimer,
    timerOrigin: String,
    assistantOrigin: String,
    themeMode: ThemeMode,
    appLanguage: AppLanguage,
    authState: AuthState,
    syncState: SyncState,
    repository: ShotRepository,
    backupFiles: BackupFiles,
    authRepository: AuthRepository,
    syncEngine: SyncEngine,
    scope: CoroutineScope,
    onNavigate: (String) -> Unit,
    onUpdateDraft: (ShotDraft) -> Unit,
    onUpdateSelectedId: (String?) -> Unit,
    onUpdateRecipeInitial: (String?) -> Unit,
    onUpdateComparisonInitial: (String?) -> Unit,
    onUpdateAssistantOrigin: (String) -> Unit,
    onUpdateTimerOrigin: (String) -> Unit,
    onUpdateThemeMode: (ThemeMode) -> Unit,
    onUpdateAppLanguage: (AppLanguage) -> Unit,
    onUpdateSaveError: (String?) -> Unit,
    onUpdateSaving: (Boolean) -> Unit,
    onNewShot: () -> Unit,
    onFavorite: (Shot, DialInContext) -> Unit,
    onRetry: () -> Unit,
    onSelectStyle: (String?) -> Unit,
    onChooseDefault: (String, String?) -> Unit
) {
    val strings = LocalStrings.current
    when (screen) {
        "recipes" -> RecipesScreen(
            recipes,
            recipeInitial,
            saving,
            saveError,
            onHistory = { onNavigate("history") },
            onSave = { recipe, done ->
                onUpdateSaving(true)
                onUpdateSaveError(null)
                scope.launch {
                    try {
                        repository.saveRecipe(recipe)
                        onUpdateRecipeInitial(null)
                        done()
                        scope.launch { syncEngine.performSync() }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        onUpdateSaveError(strings.couldNotSaveRecipe)
                    } finally {
                        onUpdateSaving(false)
                    }
                }
            },
            onDelete = { id ->
                onUpdateSaving(true)
                onUpdateSaveError(null)
                scope.launch {
                    try {
                        repository.deleteRecipe(id)
                        scope.launch { syncEngine.performSync() }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        onUpdateSaveError(strings.couldNotDeleteRecipe)
                    } finally {
                        onUpdateSaving(false)
                    }
                }
            },
            onPrepare = { recipe, withTimer ->
                onUpdateDraft(recipe.toDraft())
                timer.reset()
                onUpdateTimerOrigin("new")
                onUpdateSaveError(null)
                onNavigate(if (withTimer) "timer" else "new")
            }
        )

        "backup" -> BackupScreen(repository, backupFiles, saving, {
            onUpdateSaving(it)
        })

        "track" -> PillarScreen(
            strings.navShots,
            strings.trackPillarDesc,
            listOf(
                strings.logAShot to { onNewShot() },
                strings.historyHeader(shots.orEmpty().size) to { onNavigate("history") },
                strings.beans to { onNavigate("beans") },
                strings.savedMachines to { onNavigate("machines") },
                strings.savedGrinders to { onNavigate("grinders") },
                strings.savedCups to { onNavigate("cups") }
            )
        )

        "improve" -> PillarScreen(
            strings.navAnalysis,
            strings.improvePillarDesc,
            listOf(
                strings.dialInTitle to {
                    onUpdateAssistantOrigin("improve")
                    onUpdateSelectedId(shots.orEmpty().firstOrNull()?.id)
                    onUpdateSaveError(null)
                    onNavigate("assistant")
                },
                strings.navAnalysis to { onNavigate("analysis") },
                strings.compareAttempts to {
                    onUpdateComparisonInitial(null)
                    onUpdateSaveError(null)
                    onNavigate("compare")
                },
                strings.ratiosAndStyles to { onNavigate("tips") }
            )
        )

        "account" -> AccountScreen(
            authRepository = authRepository,
            syncEngine = syncEngine,
            onAdmin = { onNavigate("admin") },
            onBackup = { onNavigate("backup") },
            onConflicts = { onNavigate("conflicts") },
            themeMode = themeMode,
            onThemeMode = onUpdateThemeMode,
            appLanguage = appLanguage,
            onAppLanguage = onUpdateAppLanguage
        )

        "conflicts" -> {
            val conflict = syncState as? SyncState.Conflict
            if (conflict == null) {
                Text(
                    if (syncState is SyncState.Syncing) {
                        strings.resolvingConflict
                    } else {
                        strings.noPendingConflicts
                    }
                )
            } else {
                ConflictScreen(
                    conflict,
                    saving || syncState is SyncState.Syncing
                ) { record, keepLocal ->
                    onUpdateSaving(true)
                    onUpdateSaveError(null)
                    scope.launch {
                        try {
                            syncEngine.resolveConflict(
                                conflict.remote,
                                record,
                                keepLocal
                            )
                            if (syncEngine.state.value !is SyncState.Conflict) {
                                onNavigate("account")
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            onUpdateSaveError(strings.conflictKept)
                        } finally {
                            onUpdateSaving(false)
                        }
                    }
                }
                saveError?.let { Text(strings.userMessage(it)) }
            }
        }

        "admin" -> AdminScreen(authRepository)

        "analysis" -> AnalysisScreen(shots.orEmpty())

        "tips" -> TipsScreen()

        "machines" -> MachinesScreen(
            defaultId = brewDefaults.find { it.id == "machines" }?.selectedId,
            onDefault = { onChooseDefault("machines", it) },
            machines = machines,
            saving = saving,
            saveError = saveError,
            onSaveMachine = { machineId, machineDraft, onSaved ->
                onUpdateSaving(true)
                onUpdateSaveError(null)
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
                        onUpdateSaveError("No pudimos guardar la máquina.")
                    } finally {
                        onUpdateSaving(false)
                    }
                }
            },
            onDeleteMachine = { machineId ->
                onUpdateSaving(true)
                scope.launch {
                    try {
                        repository.deleteMachine(machineId)
                        scope.launch { syncEngine.performSync() }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                    } finally {
                        onUpdateSaving(false)
                    }
                }
            }
        )

        "beans" -> BeansScreen(
            defaultId = brewDefaults.find { it.id == "beans" }?.selectedId,
            onDefault = { onChooseDefault("beans", it) },
            photos = backupFiles.photos,
            canAddPhoto = authState is AuthState.LoggedIn,
            beans = beansList,
            saving = saving,
            saveError = saveError,
            onSaveBean = { beanId, beanDraft, onSaved ->
                onUpdateSaving(true)
                onUpdateSaveError(null)
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
                        onUpdateSaveError("No pudimos guardar el café.")
                    } finally {
                        onUpdateSaving(false)
                    }
                }
            },
            onDeleteBean = { beanId ->
                onUpdateSaving(true)
                scope.launch {
                    try {
                        repository.deleteBean(beanId)
                        scope.launch { syncEngine.performSync() }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                    } finally {
                        onUpdateSaving(false)
                    }
                }
            }
        )

        "grinders" -> GrindersScreen(
            defaultId = brewDefaults.find { it.id == "grinders" }?.selectedId,
            onDefault = { onChooseDefault("grinders", it) },
            grinders = grinders,
            saving = saving,
            saveError = saveError,
            onSaveGrinder = { grinderId, grinderDraft, onSaved ->
                onUpdateSaving(true)
                onUpdateSaveError(null)
                scope.launch {
                    try {
                        if (grinderId == null) {
                            repository.saveGrinder(grinderDraft)
                        } else {
                            repository.updateGrinder(grinderId, grinderDraft)
                        }
                        onSaved()
                        scope.launch { syncEngine.performSync() }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        onUpdateSaveError("No pudimos guardar el molinillo.")
                    } finally {
                        onUpdateSaving(false)
                    }
                }
            },
            onDeleteGrinder = { grinderId ->
                onUpdateSaving(true)
                scope.launch {
                    try {
                        repository.deleteGrinder(grinderId)
                        scope.launch { syncEngine.performSync() }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                    } finally {
                        onUpdateSaving(false)
                    }
                }
            }
        )

        "cups" -> CupsScreen(
            defaultId = brewDefaults.find { it.id == "cups" }?.selectedId,
            onDefault = { onChooseDefault("cups", it) },
            cups = cupsList,
            saving = saving,
            saveError = saveError,
            onSaveCup = { cupId, cupDraft, onSaved ->
                onUpdateSaving(true)
                onUpdateSaveError(null)
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
                        onUpdateSaveError("No pudimos guardar la taza.")
                    } finally {
                        onUpdateSaving(false)
                    }
                }
            },
            onDeleteCup = { cupId ->
                onUpdateSaving(true)
                scope.launch {
                    try {
                        repository.deleteCup(cupId)
                        scope.launch { syncEngine.performSync() }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                    } finally {
                        onUpdateSaving(false)
                    }
                }
            }
        )

        "compare" -> ShotComparisonScreen(
            shots.orEmpty(),
            comparisonInitial,
            saving,
            saveError
        ) {
            if (it.dialIn?.favorite != true) onFavorite(it, it.dialIn ?: DialInContext())
        }

        "assistant" -> {
            val source = shots.orEmpty().find { it.id == selectedId }
            if (source == null) {
                Column {
                    Text(
                        strings.homeNoShotsYet
                    )
                    TextButton(onClick = {
                        onNavigate("new")
                    }) { Text(strings.logAShot) }
                }
            } else {
                DialInScreen(
                    source,
                    shots.orEmpty(),
                    saving,
                    saveError,
                    onEdit = {
                        onUpdateDraft(source.toDraft())
                        onUpdateSaveError(null)
                        onNavigate("edit")
                    },
                    onFavorite = { onFavorite(source, it) },
                    onSelect = {
                        onUpdateSelectedId(it.id)
                        onUpdateSaveError(null)
                    },
                    onNext = { next, context ->
                        onUpdateSaving(true)
                        onUpdateSaveError(null)
                        scope.launch {
                            try {
                                repository.update(
                                    source.id,
                                    source.toDraft().copy(dialIn = context)
                                )
                                onUpdateDraft(next)
                                onNavigate("new")
                                scope.launch { syncEngine.performSync() }
                            } catch (
                                cancelled: CancellationException
                            ) {
                                throw cancelled
                            } catch (_: Exception) {
                                onUpdateSaveError(strings.couldNotSaveDefault)
                            } finally {
                                onUpdateSaving(false)
                            }
                        }
                    }
                )
            }
        }

        "timer" -> ShotTimerScreen(timer, timerOrigin != "home") { seconds, firstDrop ->
            onUpdateDraft(draft.copy(seconds = seconds, firstDrop = firstDrop))
            onUpdateSaveError(null)
            onNavigate(if (timerOrigin == "edit") "edit" else "new")
        }

        "new", "edit" -> ShotForm(
            draft = draft,
            machines = machines,
            beans = beansList,
            cups = cupsList,
            grinders = grinders,
            onChange = {
                onUpdateDraft(it)
                onUpdateSaveError(null)
            },
            saving = saving,
            saveError = saveError,
            isEditing = screen == "edit",
            onOpenTimer = {
                onUpdateTimerOrigin(screen)
                onNavigate("timer")
            },
            onNavigateToMachines = { onNavigate("machines") },
            onNavigateToBeans = { onNavigate("beans") },
            onNavigateToCups = { onNavigate("cups") },
            onNavigateToGrinders = { onNavigate("grinders") },
            onSave = {
                onUpdateSaving(true)
                onUpdateSaveError(null)
                scope.launch {
                    try {
                        if (screen == "edit" && selectedId != null) {
                            repository.update(selectedId, draft)
                            onNavigate("detail")
                        } else {
                            repository.save(draft)
                            onNavigate("history")
                        }
                        onUpdateDraft(ShotDraft())
                        scope.launch { syncEngine.performSync() }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        onUpdateSaveError(strings.couldNotSaveShot)
                    } finally {
                        onUpdateSaving(false)
                    }
                }
            }
        )

        "detail" -> {
            val shot = shots.orEmpty().find { it.id == selectedId }
            if (shot == null) {
                Text(strings.shotNotFound)
            } else {
                ShotDetail(
                    shot = shot,
                    beans = beansList,
                    machines = machines,
                    grinders = grinders,
                    cups = cupsList,
                    onCompare = {
                        onUpdateComparisonInitial(shot.id)
                        onUpdateSaveError(null)
                        onNavigate("compare")
                    },
                    onSaveRecipe = {
                        onUpdateRecipeInitial(
                            Json.encodeToString(
                                shot.asRecipe(
                                    Uuid.random().toString(),
                                    ("${shot.bean.name} · ${strings.knownDrinkName(shot.style ?: "Espresso")}").take(100)
                                )
                            )
                        )
                        onUpdateSaveError(null)
                        onNavigate("recipes")
                    },
                    onDialIn = {
                        onUpdateAssistantOrigin("detail")
                        onUpdateSaveError(null)
                        onNavigate("assistant")
                    },
                    onFavorite = { onFavorite(shot, shot.dialIn ?: DialInContext()) },
                    error = saveError,
                    onEdit = {
                        onUpdateDraft(
                            ShotDraft(
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
                                grinder = shot.grinder ?: shot.dialIn?.grinder.orEmpty(),
                                grinderId = shot.grinderId,
                                milkFoam = shot.milkFoam,
                                firstDrop = shot.firstDrop?.pretty().orEmpty(),
                                taste = shot.taste,
                                dialIn = shot.dialIn
                            )
                        )
                        onNavigate("edit")
                    },
                    onDelete = {
                        onUpdateSaving(true)
                        scope.launch {
                            try {
                                repository.delete(shot.id)
                                onNavigate("history")
                                scope.launch { syncEngine.performSync() }
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                            } finally {
                                onUpdateSaving(false)
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
                onRecipes = {
                    onUpdateRecipeInitial(null)
                    onUpdateSaveError(null)
                    onNavigate("recipes")
                },
                shots = shots,
                filteredShots = filteredShots,
                selectedStyle = selectedStyle,
                styleOptions = styleOptions,
                loadError = loadError,
                userName = userName,
                onSelectStyle = onSelectStyle,
                onOpenDialIn = {
                    onUpdateAssistantOrigin("home")
                    onUpdateSelectedId(shots.orEmpty().firstOrNull()?.id)
                    onUpdateSaveError(null)
                    onNavigate("assistant")
                },
                onOpenTimer = {
                    onNewShot()
                    onUpdateTimerOrigin("home")
                    onNavigate("timer")
                },
                onRepeatShot = { shot ->
                    onUpdateDraft(shot.repeatDraft())
                    onUpdateSelectedId(null)
                    timer.reset()
                    onUpdateSaveError(null)
                    onNavigate("new")
                },
                onNewShot = { onNewShot() },
                onPrepare = { prepared ->
                    onUpdateDraft(
                        brewDefaults.newShot(
                            beansList,
                            machines,
                            grinders,
                            cupsList
                        ).copy(
                            dose = prepared.dose.pretty(),
                            output = prepared.output.pretty(),
                            extraWater = if (prepared.water > 0.0) prepared.water.pretty() else "",
                            milk = if (prepared.milk > 0.0) prepared.milk.pretty() else "",
                            style = prepared.style,
                            milkFoam = prepared.milkFoam,
                            seconds = "",
                            firstDrop = "",
                            notes = "",
                            taste = null,
                            dialIn = null
                        )
                    )
                    onUpdateSelectedId(null)
                    timer.reset()
                    onUpdateSaveError(null)
                    onNavigate("new")
                },
                onSelectShot = {
                    onUpdateSelectedId(it.id)
                    onNavigate("detail")
                },
                onNavigateToHistory = { onNavigate("history") },
                onNavigateToBeans = { onNavigate("beans") },
                onNavigateToMachines = { onNavigate("machines") },
                onNavigateToCups = { onNavigate("cups") },
                onNavigateToTips = { onNavigate("tips") },
                onNavigateToBackup = { onNavigate("backup") },
                onNavigateToAnalysis = { onNavigate("analysis") },
                onRetry = onRetry,
                refreshError = refreshError
            )
        }
    }
}
