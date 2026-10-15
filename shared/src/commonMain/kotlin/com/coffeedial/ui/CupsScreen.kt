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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.coffeedial.ui.CoffeeButton as Button
import com.coffeedial.ui.CoffeeCard as Card
import com.coffeedial.ui.CoffeeTextButton as TextButton
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun CupsScreen(
    cups: List<Cup>,
    saving: Boolean,
    saveError: String?,
    onSaveCup: (String?, CupDraft, () -> Unit) -> Unit,
    onDeleteCup: (String) -> Unit,
    defaultId: String? = null,
    onDefault: ((String?) -> Unit)? = null
) {
    val strings = LocalStrings.current
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var capacity by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    fun closeEditor() {
        focusManager.clearFocus(force = true)
        editorOpen = false
    }

    val draft = CupDraft(name, weight, capacity)
    val errors = if (submitted) draft.errors(strings) else emptyMap()

    fun open(cup: Cup?) {
        editingId = cup?.id
        name = cup?.name.orEmpty()
        weight = cup?.weight?.pretty().orEmpty()
        capacity = cup?.capacity?.pretty().orEmpty()
        submitted = false
        editorOpen = true
    }

    if (!editorOpen) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(strings.savedCups, style = MaterialTheme.typography.headlineSmall)
                    Button(onClick = { open(null) }, enabled = !saving) { Text(strings.addCup) }
                }
            }
            if (cups.isEmpty()) item { Text(strings.noCupsYet) }
            items(cups, key = { it.id }) { cup ->
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(cup.name, style = MaterialTheme.typography.titleMedium)
                        val details = listOfNotNull(
                            cup.weight?.let { "${strings.emptyWeight}: ${it.pretty()} g" },
                            cup.capacity?.let { "${strings.capacity}: ${it.pretty()} cm³" }
                        ).joinToString(" · ")
                        if (details.isNotBlank()) {
                            Text(details)
                        }
                        onDefault?.let { choose ->
                            androidx.compose.material3.FilterChip(
                                selected = defaultId == cup.id,
                                onClick = { choose(if (defaultId == cup.id) null else cup.id) },
                                enabled = !saving,
                                label = {
                                    Text(
                                        if (defaultId ==
                                            cup.id
                                        ) {
                                            strings.defaultLabel
                                        } else {
                                            strings.useByDefault
                                        }
                                    )
                                }
                            )
                        }
                        FlowRow {
                            TextButton(onClick = {
                                open(cup)
                            }, enabled = !saving) { Text(strings.actionEdit) }
                            TextButton(onClick = {
                                onDeleteCup(cup.id)
                            }, enabled = !saving) {
                                Text(strings.actionDelete, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            if (saveError != null && !editorOpen) {
                item {
                    Text(strings.userMessage(saveError), color = MaterialTheme.colorScheme.error)
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
                if (editingId == null) strings.addCup else strings.editCup,
                style = MaterialTheme.typography.headlineSmall
            )
            CoffeeSection {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.nameField) },
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = errors["name"] != null,
                    supportingText = errors["name"]?.let { { Text(it) } }
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text(strings.emptyWeight) },
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = errors["weight"] != null,
                    supportingText = errors["weight"]?.let { { Text(it) } }
                )
                OutlinedTextField(
                    value = capacity,
                    onValueChange = { capacity = it },
                    label = { Text(strings.capacity) },
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = errors["capacity"] != null,
                    supportingText = errors["capacity"]?.let { { Text(it) } }
                )
                if (submitted && saveError != null) {
                    Text(strings.userMessage(saveError), color = MaterialTheme.colorScheme.error)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = !saving, onClick = {
                    submitted = true
                    if (draft.errors().isEmpty()) onSaveCup(editingId, draft) { closeEditor() }
                }) { Text(strings.actionSave) }
                TextButton(onClick = { closeEditor() }, enabled = !saving) { Text(strings.actionCancel) }
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
                Cup("1", "Taza Loveramics 150ml", 180.0, 150.0),
                Cup("2", "Vaso Duralex", 120.0, 220.0)
            ),
            saving = false,
            saveError = null,
            onSaveCup = { _, _, _ -> },
            onDeleteCup = {}
        )
    }
}
