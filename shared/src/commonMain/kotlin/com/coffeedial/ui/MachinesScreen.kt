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
import com.coffeedial.domain.Machine
import com.coffeedial.domain.MachineDraft
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun MachinesScreen(
    machines: List<Machine>,
    saving: Boolean,
    saveError: String?,
    onSaveMachine: (String?, MachineDraft, () -> Unit) -> Unit,
    onDeleteMachine: (String) -> Unit
) {
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("Espresso") }
    var year by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    fun closeEditor() {
        focusManager.clearFocus(force = true)
        editorOpen = false
    }
    val draft = MachineDraft(name, type, year)
    val errors = if (submitted) draft.errors() else emptyMap()

    fun open(machine: Machine?) {
        editingId = machine?.id
        name = machine?.name.orEmpty()
        type = machine?.type ?: "Espresso"
        year = machine?.year.orEmpty()
        submitted = false
        editorOpen = true
    }

    if (!editorOpen) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text("Máquinas de café", style = MaterialTheme.typography.headlineSmall)
                Button(onClick = { open(null) }, enabled = !saving) { Text("Agregar máquina") }
            }
            if (machines.isEmpty()) item { Text("Todavía no agregaste ninguna máquina.") }
            items(machines, key = { it.id }) { machine ->
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(machine.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            listOf(machine.type, machine.year).filter {
                                it.isNotBlank()
                            }.joinToString(" · ")
                        )
                        FlowRow {
                            TextButton(onClick = {
                                open(machine)
                            }, enabled = !saving) { Text("Editar") }
                            TextButton(onClick = {
                                onDeleteMachine(machine.id)
                            }, enabled = !saving) {
                                Text("Eliminar", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            if (saveError != null && !editorOpen) {
                item {
                    Text(saveError, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    if (editorOpen) {
        Column(
            Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (editingId ==
                    null
                ) {
                    "Nueva máquina"
                } else {
                    "Editar máquina"
                },
                style = MaterialTheme.typography.headlineSmall
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    name,
                    { name = it },
                    label = { Text("Nombre") },
                    enabled = !saving,
                    singleLine = true,
                    isError = errors["name"] != null,
                    supportingText = errors["name"]?.let { { Text(it) } }
                )
                Text("Tipo de máquina")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (listOf("Espresso", "Cápsulas", "Filtro", "Prensa") + type)
                        .distinct().forEach { option ->
                            FilterChip(
                                selected = type == option,
                                onClick = { type = option },
                                enabled = !saving,
                                label = { Text(option) }
                            )
                        }
                }
                OutlinedTextField(
                    year,
                    { year = it },
                    label = { Text("Año (opcional)") },
                    enabled = !saving,
                    singleLine = true,
                    isError = errors["year"] != null,
                    supportingText = errors["year"]?.let { { Text(it) } }
                )
                if (editingId !=
                    null
                ) {
                    Text("Los shots anteriores conservan el nombre que tenían al registrarlos.")
                }
                if (submitted &&
                    saveError != null
                ) {
                    Text(saveError, color = MaterialTheme.colorScheme.error)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(enabled = !saving, onClick = {
                    submitted = true
                    if (draft.errors().isEmpty()) onSaveMachine(editingId, draft) { closeEditor() }
                }) { Text(if (saving) "Guardando…" else "Guardar") }
                TextButton(onClick = { closeEditor() }, enabled = !saving) { Text("Cancelar") }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MachinesPreview() {
    MaterialTheme { MachinesScreen(mockMachines, false, null, { _, _, _ -> }, {}) }
}
