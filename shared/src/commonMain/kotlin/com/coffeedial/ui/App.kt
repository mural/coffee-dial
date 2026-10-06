package com.coffeedial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.coffeedial.auth.AuthRepository
import com.coffeedial.auth.AuthState
import com.coffeedial.backup.BackupFiles
import com.coffeedial.data.ShotRepository
import com.coffeedial.domain.Bean
import com.coffeedial.domain.BeanDraft
import com.coffeedial.domain.Cup
import com.coffeedial.domain.CupDraft
import com.coffeedial.domain.Machine
import com.coffeedial.domain.Shot
import com.coffeedial.domain.ShotDraft
import com.coffeedial.sync.SyncEngine
import kotlin.math.round
import kotlin.time.Instant
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
        SelectionContainer {
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

                                        "machines", "beans", "cups",
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

                        else -> when {
                            loadError -> {
                                Text("No pudimos leer tu historial.")
                                Button(onClick = { retry++ }) { Text("Reintentar") }
                            }

                            shots == null -> CircularProgressIndicator()

                            screen == "detail" -> {
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

                            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                item {
                                    if (screen == "home" && authState is AuthState.LoggedIn) {
                                        val user = (authState as AuthState.LoggedIn).user
                                        val name = user.displayName?.trim()?.takeIf {
                                            it.isNotEmpty()
                                        }
                                            ?.substringBefore(' ')
                                        Row(
                                            modifier = Modifier.padding(bottom = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                if (name == null) "¡Hola!" else "¡Hola, $name!",
                                                style = MaterialTheme.typography.titleLarge
                                            )
                                            androidx.compose.material3.Icon(
                                                CoffeeCup,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Text(
                                        if (screen ==
                                            "home"
                                        ) {
                                            "Tu próximo buen café empieza acá."
                                        } else {
                                            "Historial (${shots.orEmpty().size})"
                                        },
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                    Text(
                                        "${shots.orEmpty().size} shots · guardados en este " +
                                            "dispositivo",
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    )
                                    Button(onClick = {
                                        screen = "new"
                                    }, modifier = Modifier.fillMaxWidth()) {
                                        Text("Registrar un shot")
                                    }
                                    if (screen == "home") {
                                        androidx.compose.foundation.layout.FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            TextButton(onClick = {
                                                screen = "history"
                                            }) { Text("Historial (${shots.orEmpty().size})") }
                                            TextButton(onClick = {
                                                screen = "beans"
                                            }) { Text("Cafés") }
                                            TextButton(onClick = {
                                                screen = "machines"
                                            }) { Text("Máquinas") }
                                            TextButton(onClick = {
                                                screen = "cups"
                                            }) { Text("Tazas") }
                                            TextButton(onClick = {
                                                screen = "backup"
                                            }) { Text("Backup") }
                                            TextButton(onClick = { screen = "analysis" }) {
                                                Text("Análisis")
                                            }
                                        }
                                        Text(
                                            "Últimos shots",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                                if (screen == "history" && shots.orEmpty().isNotEmpty()) {
                                    item {
                                        Text("Estilo", style = MaterialTheme.typography.titleMedium)
                                        androidx.compose.foundation.layout.FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            FilterChip(
                                                selected = selectedStyle == null,
                                                onClick = {
                                                    selectedStyle = null
                                                },
                                                label = { Text("Todos") }
                                            )
                                            styleOptions.forEach { option ->
                                                FilterChip(
                                                    selected =
                                                        selectedStyle == option.lowercase(),
                                                    onClick = {
                                                        selectedStyle = option.lowercase()
                                                    },
                                                    label = {
                                                        Text(option.ifBlank { "Sin estilo" })
                                                    }
                                                )
                                            }
                                        }
                                        Text(
                                            "${filteredShots.size} de ${shots.orEmpty().size} shots"
                                        )
                                    }
                                    if (filteredShots.isEmpty()) {
                                        item {
                                            Text("No hay shots con este estilo.")
                                        }
                                    }
                                }
                                if (shots.orEmpty().isEmpty()) {
                                    item {
                                        Text(
                                            "Todavía no hay shots. Registrá tu receta " +
                                                "y empezá a encontrar tu punto ideal."
                                        )
                                    }
                                }
                                items(
                                    if (screen ==
                                        "home"
                                    ) {
                                        shots.orEmpty().take(3)
                                    } else {
                                        filteredShots
                                    },
                                    key = { it.id }
                                ) { shot ->
                                    ShotCard(shot) {
                                        selectedId = shot.id
                                        screen = "detail"
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShotForm(
    draft: ShotDraft,
    machines: List<Machine>,
    beans: List<Bean>,
    cups: List<Cup>,
    onChange: (ShotDraft) -> Unit,
    saving: Boolean,
    saveError: String?,
    isEditing: Boolean = false,
    onNavigateToMachines: () -> Unit,
    onNavigateToBeans: () -> Unit,
    onNavigateToCups: () -> Unit,
    onSave: () -> Unit
) {
    var submitted by rememberSaveable { mutableStateOf(false) }
    val errors = if (submitted) draft.errors() else emptyMap()
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(
                if (isEditing) "Editar shot" else "Nuevo shot",
                style = MaterialTheme.typography.headlineSmall
            )
        }
        item {
            if (beans.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Café guardado", style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = onNavigateToBeans, enabled = !saving) {
                            Text("Gestionar cafés")
                        }
                    }
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        beans.forEach { b ->
                            FilterChip(
                                selected = draft.beanName == b.name,
                                onClick = {
                                    onChange(
                                        draft.copy(
                                            beanName = b.name,
                                            roaster = b.roaster
                                        )
                                    )
                                },
                                label = {
                                    Text(
                                        if (b.roaster.isNotBlank()) {
                                            "${b.name} (${b.roaster})"
                                        } else {
                                            b.name
                                        }
                                    )
                                },
                                enabled = !saving
                            )
                        }
                    }
                    Field("Nombre del café", draft.beanName, errors["beanName"], saving) {
                        onChange(draft.copy(beanName = it))
                    }
                    Field("Tostador (opcional)", draft.roaster, null, saving) {
                        onChange(draft.copy(roaster = it))
                    }
                }
            } else {
                Column {
                    Field("Café", draft.beanName, errors["beanName"], saving) {
                        onChange(draft.copy(beanName = it))
                    }
                    Field("Tostador (opcional)", draft.roaster, null, saving) {
                        onChange(draft.copy(roaster = it))
                    }
                    TextButton(onClick = onNavigateToBeans, enabled = !saving) {
                        Text("+ Agregar café guardado")
                    }
                }
            }
        }
        item {
            Field("Dosis de entrada · g", draft.dose, errors["dose"], saving, true) {
                onChange(draft.copy(dose = it))
            }
        }
        item {
            Field("Output · g", draft.output, errors["output"], saving, true) {
                onChange(draft.copy(output = it))
            }
        }
        item {
            Field("Estilo (opcional)", draft.style, errors["style"], saving) {
                onChange(draft.copy(style = it))
            }
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Espresso", "Lungo", "Americano", "Latte", "Cappuccino").forEach { style ->
                    androidx.compose.material3.FilterChip(
                        selected = draft.style == style,
                        onClick = {
                            onChange(
                                draft.copy(
                                    style = if (draft.style ==
                                        style
                                    ) {
                                        ""
                                    } else {
                                        style
                                    }
                                )
                            )
                        },
                        enabled = !saving,
                        label = { Text(style) }
                    )
                }
            }
        }
        item {
            Field(
                "Agua extra · ml (opcional)",
                draft.extraWater,
                errors["extraWater"],
                saving,
                true
            ) {
                onChange(draft.copy(extraWater = it))
            }
            Text(
                "Agua agregada después de la extracción; no modifica el ratio.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        item {
            Field("Leche · ml (opcional)", draft.milk, errors["milk"], saving, true) {
                onChange(draft.copy(milk = it))
            }
        }
        item {
            if (machines.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Máquina", style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = onNavigateToMachines, enabled = !saving) {
                            Text("Gestionar máquinas")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        machines.forEach { m ->
                            FilterChip(
                                selected = draft.machine == m.name,
                                onClick = {
                                    onChange(
                                        draft.copy(
                                            machine = if (draft.machine ==
                                                m.name
                                            ) {
                                                ""
                                            } else {
                                                m.name
                                            }
                                        )
                                    )
                                },
                                label = { Text(m.name) },
                                enabled = !saving
                            )
                        }
                    }
                    Field("O escribir máquina manualmente", draft.machine, null, saving) {
                        onChange(draft.copy(machine = it))
                    }
                }
            } else {
                Column {
                    Field("Máquina (opcional)", draft.machine, null, saving) {
                        onChange(draft.copy(machine = it))
                    }
                    TextButton(onClick = onNavigateToMachines, enabled = !saving) {
                        Text("+ Agregar máquina guardada")
                    }
                }
            }
        }
        item {
            if (cups.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Taza", style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = onNavigateToCups, enabled = !saving) {
                            Text("Gestionar tazas")
                        }
                    }
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        cups.forEach { c ->
                            FilterChip(
                                selected = draft.cup == c.name,
                                onClick = {
                                    onChange(
                                        draft.copy(
                                            cup = if (draft.cup == c.name) "" else c.name
                                        )
                                    )
                                },
                                label = {
                                    Text(
                                        if (c.weight !=
                                            null
                                        ) {
                                            "${c.name} (${c.weight.pretty()} g)"
                                        } else {
                                            c.name
                                        }
                                    )
                                },
                                enabled = !saving
                            )
                        }
                    }
                    Field("O escribir taza manualmente", draft.cup, null, saving) {
                        onChange(draft.copy(cup = it))
                    }
                }
            } else {
                Column {
                    Field("Taza (opcional)", draft.cup, null, saving) {
                        onChange(draft.copy(cup = it))
                    }
                    TextButton(onClick = onNavigateToCups, enabled = !saving) {
                        Text("+ Agregar taza guardada")
                    }
                }
            }
        }
        item {
            Field("Tiempo · s", draft.seconds, errors["seconds"], saving, true) {
                onChange(draft.copy(seconds = it))
            }
        }
        item {
            Field("Molienda · ajuste del molino", draft.grind, errors["grind"], saving) {
                onChange(draft.copy(grind = it))
            }
        }
        item {
            Field(
                "Temperatura · °C (opcional)",
                draft.temperature,
                errors["temperature"],
                saving,
                true
            ) {
                onChange(draft.copy(temperature = it))
            }
        }
        item {
            Field("Tostador (opcional)", draft.roaster, null, saving) {
                onChange(draft.copy(roaster = it))
            }
        }
        item {
            OutlinedTextField(
                value = draft.notes,
                onValueChange = { onChange(draft.copy(notes = it)) },
                label = {
                    Text("Notas (opcional)")
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                trailingIcon = if (draft.notes.isNotEmpty() && !saving) {
                    {
                        androidx.compose.material3.IconButton(onClick = {
                            onChange(draft.copy(notes = ""))
                        }) {
                            ClearIcon("notas")
                        }
                    }
                } else {
                    null
                }
            )
        }
        item {
            Text("¿Qué tal salió? · 1 a 5")
            CoffeeRating(draft.rating, onChange = {
                onChange(draft.copy(rating = it))
            }, enabled = !saving)
        }
        if (saveError != null) item { Text(saveError, color = MaterialTheme.colorScheme.error) }
        item {
            Button(
                onClick = {
                    submitted = true
                    if (draft.errors().isEmpty()) onSave()
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                Text(
                    if (saving) {
                        "Guardando…"
                    } else if (isEditing) {
                        "Guardar cambios"
                    } else {
                        "Guardar shot"
                    }
                )
            }
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    error: String?,
    saving: Boolean,
    decimal: Boolean = false,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        modifier = Modifier.fillMaxWidth(), enabled = !saving, singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Text
        ),
        isError = error != null, supportingText = error?.let { { Text(it) } },
        trailingIcon = if (value.isNotEmpty() && !saving) {
            {
                androidx.compose.material3.IconButton(onClick = { onChange("") }) {
                    ClearIcon(label)
                }
            }
        } else {
            null
        }
    )
}

@Composable
private fun ShotDetail(shot: Shot, onEdit: () -> Unit, onDelete: () -> Unit, saving: Boolean) {
    var showConfirm by remember { mutableStateOf(false) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text(shot.bean.name, style = MaterialTheme.typography.headlineLarge) }
        if (shot.bean.roaster.isNotBlank()) item { Text(shot.bean.roaster) }
        if (!shot.machine.isNullOrBlank()) {
            item { Text("Máquina: ${shot.machine}") }
        }
        if (!shot.cup.isNullOrBlank()) {
            item { Text("Taza: ${shot.cup}") }
        }
        item { Text(timestamp(shot.createdAt)) }
        item { Text("1:${shot.ratio.pretty()}", style = MaterialTheme.typography.displayMedium) }
        item { Text("Ratio de extracción") }
        item {
            val total = shot.output + (shot.milk ?: 0.0) + (shot.extraWater ?: 0.0)
            Text(
                "Entrada: ${shot.dose.pretty()} g\n" +
                    "Output: ${shot.output.pretty()} g" +
                    (shot.style?.let { "\nEstilo: $it" } ?: "") +
                    (shot.extraWater?.let { "\nAgua extra: ${it.pretty()} ml" } ?: "") +
                    (shot.milk?.let { "\nLeche: ${it.pretty()} ml" } ?: "") +
                    (
                        if (shot.milk != null ||
                            shot.extraWater != null
                        ) {
                            "\nTotal: ${total.pretty()} g"
                        } else {
                            ""
                        }
                        ) +
                    "\nTiempo: ${shot.seconds.pretty()} s"
            )
        }
        item { Text("Molienda: ${shot.grind}") }
        item {
            Text("Temperatura: ${shot.temperature?.let { "${it.pretty()} °C" } ?: "Sin registrar"}")
        }
        item { CoffeeRating(shot.rating) }
        item {
            Text(shot.notes.ifBlank { "Sin notas" })
        }
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                Button(
                    onClick = onEdit,
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Editar shot")
                }
                if (showConfirm) {
                    Text("¿Querés eliminar este shot?", color = MaterialTheme.colorScheme.error)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onDelete,
                            enabled = !saving,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Sí, eliminar")
                        }
                        OutlinedButton(
                            onClick = { showConfirm = false },
                            enabled = !saving,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar")
                        }
                    }
                } else {
                    TextButton(
                        onClick = { showConfirm = true },
                        enabled = !saving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Eliminar shot", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// COMPOSE PREVIEWS
// -----------------------------------------------------------------------------

@Preview(name = "Tarjeta de Shot", showBackground = true)
@Composable
private fun ShotCardPreview() {
    MaterialTheme {
        ShotCard(
            shot = mockShot,
            onClick = {}
        )
    }
}

@Preview(name = "Detalle del Shot", showBackground = true)
@Composable
private fun ShotDetailPreview() {
    MaterialTheme {
        ShotDetail(shot = mockShot, onEdit = {}, onDelete = {}, saving = false)
    }
}

@Preview(name = "Formulario - Vacío", showBackground = true)
@Composable
private fun ShotFormEmptyPreview() {
    MaterialTheme {
        ShotForm(
            draft = ShotDraft(),
            machines = mockMachines,
            beans = listOf(mockBean),
            cups = emptyList(),
            onChange = {},
            saving = false,
            saveError = null,
            onNavigateToMachines = {},
            onNavigateToBeans = {},
            onNavigateToCups = {},
            onSave = {}
        )
    }
}

@Preview(name = "Formulario - Con Datos", showBackground = true)
@Composable
private fun ShotFormFilledPreview() {
    MaterialTheme {
        ShotForm(
            draft = mockDraft,
            machines = mockMachines,
            beans = listOf(mockBean),
            cups = emptyList(),
            onChange = {},
            saving = false,
            saveError = null,
            onNavigateToMachines = {},
            onNavigateToBeans = {},
            onNavigateToCups = {},
            onSave = {}
        )
    }
}

@Preview(name = "Formulario - Guardando (Loading)", showBackground = true)
@Composable
private fun ShotFormSavingPreview() {
    MaterialTheme {
        ShotForm(
            draft = mockDraft,
            machines = mockMachines,
            beans = listOf(mockBean),
            cups = emptyList(),
            onChange = {},
            saving = true,
            saveError = null,
            onNavigateToMachines = {},
            onNavigateToBeans = {},
            onNavigateToCups = {},
            onSave = {}
        )
    }
}

@Preview(name = "Formulario - Con Error de Guardado", showBackground = true)
@Composable
private fun ShotFormErrorPreview() {
    MaterialTheme {
        ShotForm(
            draft = mockDraft,
            machines = mockMachines,
            beans = listOf(mockBean),
            cups = emptyList(),
            onChange = {},
            saving = false,
            saveError = "No pudimos guardar el shot. Tus datos siguen acá; intentá otra vez.",
            onNavigateToMachines = {},
            onNavigateToBeans = {},
            onNavigateToCups = {},
            onSave = {}
        )
    }
}

@Preview(name = "Campo de Texto Individual", showBackground = true)
@Composable
private fun FieldPreview() {
    MaterialTheme {
        Field(
            label = "Dosis de entrada · g",
            value = "18.5",
            error = null,
            saving = false,
            decimal = true,
            onChange = {}
        )
    }
}
