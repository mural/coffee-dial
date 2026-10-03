package com.coffeedial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.coffeedial.backup.BackupFiles
import com.coffeedial.data.ShotRepository
import com.coffeedial.domain.Shot
import com.coffeedial.domain.ShotDraft
import kotlin.math.round
import kotlin.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

private val DraftSaver = Saver<ShotDraft, List<String>>(
    save = {
        listOf(
            it.beanName, it.roaster, it.dose, it.output, it.seconds,
            it.grind, it.temperature, it.milk, it.notes, it.rating.toString()
        )
    },
    restore = { ShotDraft(it[0], it[1], it[2], it[3], it[4], it[5], it[6], it[7], it[8], it[9].toInt()) }
)

@Composable
fun App(repository: ShotRepository, backupFiles: BackupFiles) {
    var screen by rememberSaveable { mutableStateOf("home") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var draft by rememberSaveable(stateSaver = DraftSaver) { mutableStateOf(ShotDraft()) }
    var shots by remember { mutableStateOf<List<Shot>?>(null) }
    var loadError by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    PlatformBack(enabled = screen != "home") {
        if (!saving && !backupFiles.busy) {
            screen = when (screen) {
                "detail" -> "history"
                "edit" -> "detail"
                else -> "home"
            }
        }
    }
    LaunchedEffect(repository, retry) {
        loadError = false
        try {
            repository.history.collect { shots = it }
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
                Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp).imePadding()
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "Coffee Dial",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                    if (screen != "home") {
                        TextButton(enabled = !saving && !backupFiles.busy, onClick = {
                            screen = when (screen) {
                                "detail" -> "history"
                                "edit" -> "detail"
                                else -> "home"
                            }
                        }) {
                            Text("Volver")
                        }
                    }
                }
                when (screen) {
                    "backup" -> BackupScreen(repository, backupFiles, saving, { saving = it })

                    "new", "edit" -> ShotForm(
                        draft,
                        {
                            draft = it
                            saveError = null
                        },
                        saving,
                        saveError,
                        isEditing = screen == "edit",
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
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (_: Exception) {
                                    saveError =
                                        "No pudimos guardar el shot. Tus datos siguen acá; intentá otra vez."
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
                                            notes = shot.notes,
                                            rating = shot.rating
                                        )
                                        screen = "edit"
                                    },
                                    onDelete = {
                                        saving = true
                                        scope.launch {
                                            try {
                                                repository.delete(shot.id)
                                                screen = "history"
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
                                Text(
                                    if (screen ==
                                        "home"
                                    ) {
                                        "Tu próximo buen café empieza acá."
                                    } else {
                                        "Historial"
                                    },
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Text(
                                    "${shots.orEmpty().size} shots · guardados en este dispositivo",
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                                Button(onClick = {
                                    screen = "new"
                                }, modifier = Modifier.fillMaxWidth()) { Text("Registrar un shot") }
                                if (screen == "home") {
                                    TextButton(onClick = {
                                        screen = "history"
                                    }) { Text("Ver todo el historial") }
                                    TextButton(onClick = { screen = "backup" }) { Text("Backup") }
                                    Text(
                                        "Últimos shots",
                                        style = MaterialTheme.typography.titleMedium
                                    )
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
                                    shots.orEmpty()
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

@Composable
private fun ShotForm(
    draft: ShotDraft,
    onChange: (ShotDraft) -> Unit,
    saving: Boolean,
    saveError: String?,
    isEditing: Boolean = false,
    onSave: () -> Unit
) {
    var submitted by rememberSaveable { mutableStateOf(false) }
    val errors = if (submitted) draft.errors() else emptyMap()
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text(if (isEditing) "Editar shot" else "Nuevo shot", style = MaterialTheme.typography.headlineSmall) }
        item {
            Field("Café", draft.beanName, errors["beanName"], saving) {
                onChange(draft.copy(beanName = it))
            }
        }
        item {
            Field("Tostador (opcional)", draft.roaster, null, saving) {
                onChange(draft.copy(roaster = it))
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
            Field("Leche · ml (opcional)", draft.milk, errors["milk"], saving, true) {
                onChange(draft.copy(milk = it))
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
                        androidx.compose.material3.IconButton(onClick = { onChange(draft.copy(notes = "")) }) {
                            Text("✕", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else null
            )
        }
        item {
            Text("¿Qué tal salió? · 1 a 5")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { value ->
                    FilterChip(selected = draft.rating == value, onClick = {
                        onChange(draft.copy(rating = value))
                    }, label = { Text("$value") }, enabled = !saving)
                }
            }
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
            ) { Text(if (saving) "Guardando…" else if (isEditing) "Guardar cambios" else "Guardar shot") }
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
                    Text("✕", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else null
    )
}

@Composable
private fun ShotDetail(shot: Shot, onEdit: () -> Unit, onDelete: () -> Unit, saving: Boolean) {
    var showConfirm by remember { mutableStateOf(false) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text(shot.bean.name, style = MaterialTheme.typography.headlineLarge) }
        if (shot.bean.roaster.isNotBlank()) item { Text(shot.bean.roaster) }
        item { Text(timestamp(shot.createdAt)) }
        item { Text("1:${shot.ratio.pretty()}", style = MaterialTheme.typography.displayMedium) }
        item { Text("Ratio de extracción") }
        item {
            Text(
                "Entrada: ${shot.dose.pretty()} g\nOutput: ${shot.output.pretty()} g${shot.milk?.let { "\nLeche: ${it.pretty()} ml" } ?: ""}\nTiempo: ${shot.seconds.pretty()} s"
            )
        }
        item { Text("Molienda: ${shot.grind}") }
        item {
            Text("Temperatura: ${shot.temperature?.let { "${it.pretty()} °C" } ?: "Sin registrar"}")
        }
        item { Text("Valoración: ${shot.rating}/5") }
        item {
            Text(shot.notes.ifBlank { "Sin notas" })
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
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
            onChange = {},
            saving = false,
            saveError = null,
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
            onChange = {},
            saving = false,
            saveError = null,
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
            onChange = {},
            saving = true,
            saveError = null,
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
            onChange = {},
            saving = false,
            saveError = "No pudimos guardar el shot. Tus datos siguen acá; intentá otra vez.",
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
