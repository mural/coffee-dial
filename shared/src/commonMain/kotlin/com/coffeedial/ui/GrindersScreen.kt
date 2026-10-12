package com.coffeedial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.coffeedial.domain.Grinder
import com.coffeedial.domain.GrinderDraft
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun GrindersScreen(
    grinders: List<Grinder>,
    saving: Boolean,
    saveError: String?,
    onSaveGrinder: (String?, GrinderDraft, () -> Unit) -> Unit,
    onDeleteGrinder: (String) -> Unit,
    defaultId: String? = null,
    onDefault: ((String?) -> Unit)? = null
) {
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("Eléctrico") }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    fun closeEditor() {
        focusManager.clearFocus(force = true)
        editorOpen = false
    }
    val draft = GrinderDraft(name, type)
    val errors = if (submitted) draft.errors() else emptyMap()

    fun open(grinder: Grinder?) {
        editingId = grinder?.id
        name = grinder?.name.orEmpty()
        type = grinder?.type ?: "Eléctrico"
        submitted = false
        editorOpen = true
    }

    if (!editorOpen) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text("Molinillos", style = MaterialTheme.typography.headlineSmall)
                Button(onClick = { open(null) }, enabled = !saving) { Text("Agregar molinillo") }
            }
            if (grinders.isEmpty()) item { Text("Todavía no agregaste ningún molinillo.") }
            items(grinders, key = { it.id }) { grinder ->
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(grinder.name, style = MaterialTheme.typography.titleMedium)
                        Text(grinder.type)
                        onDefault?.let { choose ->
                            androidx.compose.material3.FilterChip(
                                selected = defaultId == grinder.id,
                                onClick = {
                                    choose(
                                        if (defaultId ==
                                            grinder.id
                                        ) {
                                            null
                                        } else {
                                            grinder.id
                                        }
                                    )
                                },
                                enabled = !saving,
                                label = {
                                    Text(
                                        if (defaultId ==
                                            grinder.id
                                        ) {
                                            "Predeterminado"
                                        } else {
                                            "Usar por defecto"
                                        }
                                    )
                                }
                            )
                        }
                        FlowRow {
                            TextButton(onClick = { open(grinder) }, enabled = !saving) {
                                Text("Editar")
                            }
                            TextButton(onClick = {
                                onDeleteGrinder(grinder.id)
                            }, enabled = !saving) {
                                Text("Eliminar", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            if (saveError != null && !editorOpen) {
                item { Text(saveError, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
    if (editorOpen) {
        Column(
            Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (editingId == null) "Nuevo molinillo" else "Editar molinillo",
                style = MaterialTheme.typography.headlineSmall
            )
            OutlinedTextField(
                name,
                { name = it },
                label = { Text("Nombre") },
                enabled = !saving,
                singleLine = true,
                isError = errors["name"] != null,
                supportingText = errors["name"]?.let { { Text(it) } }
            )
            Text("Tipo de molinillo")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Eléctrico", "Manual").forEach { option ->
                    FilterChip(
                        selected = type == option,
                        onClick = { type = option },
                        enabled = !saving,
                        label = { Text(option) }
                    )
                }
            }
            if (editingId != null) {
                Text(
                    "Si cambiás el nombre, los shots vinculados muestran el nuevo. " +
                        "Si lo eliminás, el shot conserva el último nombre."
                )
            }
            if (submitted && saveError != null) {
                Text(saveError, color = MaterialTheme.colorScheme.error)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(enabled = !saving, onClick = {
                    submitted = true
                    if (draft.errors().isEmpty()) onSaveGrinder(editingId, draft) { closeEditor() }
                }) { Text(if (saving) "Guardando…" else "Guardar") }
                TextButton(onClick = { closeEditor() }, enabled = !saving) { Text("Cancelar") }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GrindersPreview() {
    MaterialTheme { GrindersScreen(mockGrinders, false, null, { _, _, _ -> }, {}) }
}
