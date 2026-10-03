package com.coffeedial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeedial.domain.Machine
import com.coffeedial.domain.MachineDraft
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun MachinesScreen(
    machines: List<Machine>,
    saving: Boolean,
    saveError: String?,
    onSaveMachine: (MachineDraft) -> Unit,
    onDeleteMachine: (String) -> Unit
) {
    var draft by remember { mutableStateOf(MachineDraft()) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val errors = if (submitted) draft.errors() else emptyMap()

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Máquinas de café", style = MaterialTheme.typography.headlineSmall)
        }

        if (machines.isEmpty()) {
            item {
                Text("Todavía no agregaste ninguna máquina.")
            }
        } else {
            items(machines, key = { it.id }) { machine ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(machine.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${machine.type}${if (machine.year.isNotBlank()) " · ${machine.year}" else ""}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        TextButton(
                            onClick = { onDeleteMachine(machine.id) },
                            enabled = !saving
                        ) {
                            Text("Eliminar", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        item {
            Text("Nueva máquina", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
        }

        item {
            OutlinedTextField(
                value = draft.name,
                onValueChange = { draft = draft.copy(name = it) },
                label = { Text("Nombre de la máquina") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !saving,
                singleLine = true,
                isError = errors["name"] != null,
                supportingText = errors["name"]?.let { { Text(it) } },
                trailingIcon = if (draft.name.isNotEmpty() && !saving) {
                    {
                        androidx.compose.material3.IconButton(onClick = { draft = draft.copy(name = "") }) {
                            Text("✕", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else null
            )
        }

        item {
            Text("Tipo de máquina")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Espresso", "Cápsulas", "Filtro", "Prensa").forEach { type ->
                    FilterChip(
                        selected = draft.type == type,
                        onClick = { draft = draft.copy(type = type) },
                        label = { Text(type) },
                        enabled = !saving
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = draft.year,
                onValueChange = { draft = draft.copy(year = it) },
                label = { Text("Año (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !saving,
                singleLine = true,
                isError = errors["year"] != null,
                supportingText = errors["year"]?.let { { Text(it) } },
                trailingIcon = if (draft.year.isNotEmpty() && !saving) {
                    {
                        androidx.compose.material3.IconButton(onClick = { draft = draft.copy(year = "") }) {
                            Text("✕", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else null
            )
        }

        if (saveError != null) {
            item {
                Text(saveError, color = MaterialTheme.colorScheme.error)
            }
        }

        item {
            Button(
                onClick = {
                    submitted = true
                    if (draft.errors().isEmpty()) {
                        onSaveMachine(draft)
                        draft = MachineDraft()
                        submitted = false
                    }
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                Text(if (saving) "Guardando…" else "Guardar máquina")
            }
        }
    }
}

@Preview(name = "Pantalla de Máquinas - Lista", showBackground = true)
@Composable
private fun MachinesScreenPreview() {
    MaterialTheme {
        MachinesScreen(
            machines = mockMachines,
            saving = false,
            saveError = null,
            onSaveMachine = {},
            onDeleteMachine = {}
        )
    }
}

@Preview(name = "Pantalla de Máquinas - Vacía", showBackground = true)
@Composable
private fun MachinesScreenEmptyPreview() {
    MaterialTheme {
        MachinesScreen(
            machines = emptyList(),
            saving = false,
            saveError = null,
            onSaveMachine = {},
            onDeleteMachine = {}
        )
    }
}
