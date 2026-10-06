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
import com.coffeedial.domain.Bean
import com.coffeedial.domain.BeanDraft
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun BeansScreen(
    beans: List<Bean>,
    saving: Boolean,
    saveError: String?,
    onSaveBean: (String?, BeanDraft, () -> Unit) -> Unit,
    onDeleteBean: (String) -> Unit
) {
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var roaster by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    fun closeEditor() {
        focusManager.clearFocus(force = true)
        editorOpen = false
    }

    val draft = BeanDraft(name, roaster)
    val errors = if (submitted) draft.errors() else emptyMap()

    fun open(bean: Bean?) {
        editingId = bean?.id
        name = bean?.name.orEmpty()
        roaster = bean?.roaster.orEmpty()
        submitted = false
        editorOpen = true
    }

    if (!editorOpen) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text("Cafés guardados", style = MaterialTheme.typography.headlineSmall)
                Text("Eliminar un café no borra los shots registrados.")
                Button(onClick = { open(null) }, enabled = !saving) { Text("Agregar café") }
            }
            if (beans.isEmpty()) item { Text("Todavía no agregaste ningún café.") }
            items(beans, key = { it.id }) { bean ->
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(bean.name, style = MaterialTheme.typography.titleMedium)
                        if (bean.roaster.isNotBlank()) {
                            Text("Tostador: ${bean.roaster}")
                        }
                        FlowRow {
                            TextButton(onClick = {
                                open(bean)
                            }, enabled = !saving) { Text("Editar") }
                            TextButton(onClick = {
                                onDeleteBean(bean.id)
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
                if (editingId == null) "Nuevo café" else "Editar café",
                style = MaterialTheme.typography.headlineSmall
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del café") },
                    enabled = !saving,
                    singleLine = true,
                    isError = errors["name"] != null,
                    supportingText = errors["name"]?.let { { Text(it) } }
                )
                OutlinedTextField(
                    value = roaster,
                    onValueChange = { roaster = it },
                    label = { Text("Tostador (opcional)") },
                    enabled = !saving,
                    singleLine = true
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
                    if (draft.errors().isEmpty()) onSaveBean(editingId, draft) { closeEditor() }
                }) { Text(if (saving) "Guardando…" else "Guardar") }
                TextButton(onClick = { closeEditor() }, enabled = !saving) { Text("Cancelar") }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BeansPreview() {
    MaterialTheme {
        BeansScreen(
            beans = listOf(mockBean, Bean("2", "Colombia Excelso", "Puerto Blest")),
            saving = false,
            saveError = null,
            onSaveBean = { _, _, _ -> },
            onDeleteBean = {}
        )
    }
}
