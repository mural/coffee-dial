package com.coffeedial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.coffeedial.domain.Cup
import com.coffeedial.domain.CupDraft
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun CupsScreen(
    cups: List<Cup>,
    saving: Boolean,
    saveError: String?,
    onSaveCup: (String?, CupDraft, () -> Unit) -> Unit,
    onDeleteCup: (String) -> Unit
) {
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    fun closeEditor() {
        focusManager.clearFocus(force = true)
        editorOpen = false
    }

    val draft = CupDraft(name, weight)
    val errors = if (submitted) draft.errors() else emptyMap()

    fun open(cup: Cup?) {
        editingId = cup?.id
        name = cup?.name.orEmpty()
        weight = cup?.weight?.pretty().orEmpty()
        submitted = false
        editorOpen = true
    }

    if (!editorOpen) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text("Tazas guardadas", style = MaterialTheme.typography.headlineSmall)
                Button(onClick = { open(null) }, enabled = !saving) { Text("Agregar taza") }
            }
            if (cups.isEmpty()) item { Text("Todavía no agregaste ninguna taza.") }
            items(cups, key = { it.id }) { cup ->
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(cup.name, style = MaterialTheme.typography.titleMedium)
                        if (cup.weight != null) {
                            Text("Peso: ${cup.weight.pretty()} g")
                        }
                        FlowRow {
                            TextButton(onClick = {
                                open(cup)
                            }, enabled = !saving) { Text("Editar") }
                            TextButton(onClick = {
                                onDeleteCup(cup.id)
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
                if (editingId == null) "Nueva taza" else "Editar taza",
                style = MaterialTheme.typography.headlineSmall
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la taza") },
                    enabled = !saving,
                    singleLine = true,
                    isError = errors["name"] != null,
                    supportingText = errors["name"]?.let { { Text(it) } }
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Peso de la taza · g (opcional)") },
                    enabled = !saving,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = errors["weight"] != null,
                    supportingText = errors["weight"]?.let { { Text(it) } }
                )
                if (editingId != null) {
                    Text(
                        "Los shots anteriores conservan la información que tenían al registrarlos."
                    )
                }
                if (submitted && saveError != null) {
                    Text(saveError, color = MaterialTheme.colorScheme.error)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(enabled = !saving, onClick = {
                    submitted = true
                    if (draft.errors().isEmpty()) onSaveCup(editingId, draft) { closeEditor() }
                }) { Text(if (saving) "Guardando…" else "Guardar") }
                TextButton(onClick = { closeEditor() }, enabled = !saving) { Text("Cancelar") }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CupsPreview() {
    MaterialTheme {
        CupsScreen(
            cups = listOf(
                Cup("1", "Taza Loveramics 150ml", 180.0),
                Cup("2", "Vaso Duralex", 120.0)
            ),
            saving = false,
            saveError = null,
            onSaveCup = { _, _, _ -> },
            onDeleteCup = {}
        )
    }
}
