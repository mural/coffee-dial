package com.coffeedial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.coffeedial.domain.Bean
import com.coffeedial.domain.Cup
import com.coffeedial.domain.Machine
import com.coffeedial.domain.ShotDraft
import com.coffeedial.domain.decimal
import kotlin.math.roundToInt
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ShotForm(
    draft: ShotDraft,
    machines: List<Machine>,
    beans: List<Bean>,
    cups: List<Cup>,
    onChange: (ShotDraft) -> Unit,
    saving: Boolean,
    saveError: String?,
    modifier: Modifier = Modifier,
    isEditing: Boolean = false,
    onNavigateToMachines: () -> Unit,
    onNavigateToBeans: () -> Unit,
    onNavigateToCups: () -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        ShotFormContent(
            draft = draft,
            machines = machines,
            beans = beans,
            cups = cups,
            onChange = onChange,
            saving = saving,
            saveError = saveError,
            isEditing = isEditing,
            onNavigateToMachines = onNavigateToMachines,
            onNavigateToBeans = onNavigateToBeans,
            onNavigateToCups = onNavigateToCups,
            onSave = onSave
        )
    }
}

@Composable
fun ShotFormContent(
    draft: ShotDraft,
    machines: List<Machine>,
    beans: List<Bean>,
    cups: List<Cup>,
    onChange: (ShotDraft) -> Unit,
    saving: Boolean,
    saveError: String?,
    modifier: Modifier = Modifier,
    isEditing: Boolean = false,
    onNavigateToMachines: () -> Unit,
    onNavigateToBeans: () -> Unit,
    onNavigateToCups: () -> Unit,
    onSave: () -> Unit
) {
    var submitted by rememberSaveable { mutableStateOf(false) }
    val errors = if (submitted) draft.errors() else emptyMap()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            if (isEditing) "Editar shot" else "Nuevo shot",
            style = MaterialTheme.typography.headlineSmall
        )

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
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

        DoseSliderField(draft.dose, errors["dose"], saving) {
            onChange(draft.copy(dose = it))
        }

        OutputSliderField(draft.output, errors["output"], saving) {
            onChange(draft.copy(output = it))
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Field("Estilo (opcional)", draft.style, errors["style"], saving) {
                onChange(draft.copy(style = it))
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Espresso", "Lungo", "Americano", "Latte", "Cappuccino").forEach { style ->
                    FilterChip(
                        selected = draft.style == style,
                        onClick = {
                            onChange(
                                draft.copy(
                                    style = if (draft.style == style) "" else style
                                )
                            )
                        },
                        enabled = !saving,
                        label = { Text(style) }
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MlSliderField("Agua extra", draft.extraWater, errors["extraWater"], saving) {
                onChange(draft.copy(extraWater = it))
            }
            Text(
                "Agua agregada después de la extracción; no modifica el ratio.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        MlSliderField("Leche", draft.milk, errors["milk"], saving) {
            onChange(draft.copy(milk = it))
        }

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
                                        machine = if (draft.machine == m.name) "" else m.name
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
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                    if (c.weight != null) {
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

        TimeSliderField(draft.seconds, errors["seconds"], saving) {
            onChange(draft.copy(seconds = it))
        }

        Field("Molienda · ajuste del molino", draft.grind, errors["grind"], saving) {
            onChange(draft.copy(grind = it))
        }

        Field(
            "Temperatura · °C (opcional)",
            draft.temperature,
            errors["temperature"],
            saving,
            true
        ) {
            onChange(draft.copy(temperature = it))
        }

        Field("Tostador (opcional)", draft.roaster, null, saving) {
            onChange(draft.copy(roaster = it))
        }

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
                    IconButton(onClick = {
                        onChange(draft.copy(notes = ""))
                    }) {
                        ClearIcon("notas")
                    }
                }
            } else {
                null
            }
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("¿Qué tal salió? · 1 a 5")
            CoffeeRating(draft.rating, onChange = {
                onChange(draft.copy(rating = it))
            }, enabled = !saving)
        }

        if (saveError != null) {
            Text(saveError, color = MaterialTheme.colorScheme.error)
        }

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

@Composable
private fun DoseSliderField(
    dose: String,
    error: String?,
    saving: Boolean,
    onChange: (String) -> Unit
) {
    val currentDoseFloat = dose.decimal()?.toFloat()?.coerceIn(10f, 40f) ?: 18f

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Dosis de entrada", style = MaterialTheme.typography.bodyMedium)
            Text(
                "${currentDoseFloat.roundToInt()} g",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = currentDoseFloat,
            onValueChange = { newValue ->
                onChange(newValue.roundToInt().toString())
            },
            valueRange = 10f..40f,
            steps = 29,
            enabled = !saving,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("10 g", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = currentDoseFloat.roundToInt() == 18,
                    onClick = { onChange("18") },
                    enabled = !saving,
                    label = { Text("18 g") }
                )
                FilterChip(
                    selected = currentDoseFloat.roundToInt() == 36,
                    onClick = { onChange("36") },
                    enabled = !saving,
                    label = { Text("36 g") }
                )
            }

            Text("40 g", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun OutputSliderField(
    output: String,
    error: String?,
    saving: Boolean,
    onChange: (String) -> Unit
) {
    val currentOutputFloat = output.decimal()?.toFloat()?.coerceIn(10f, 100f) ?: 36f

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Output", style = MaterialTheme.typography.bodyMedium)
            Text(
                "${currentOutputFloat.roundToInt()} g",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = currentOutputFloat,
            onValueChange = { newValue ->
                onChange(newValue.roundToInt().toString())
            },
            valueRange = 10f..100f,
            steps = 89,
            enabled = !saving,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("10 g", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("100 g", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun MlSliderField(
    label: String,
    value: String,
    error: String?,
    saving: Boolean,
    onChange: (String) -> Unit
) {
    val currentMlFloat = value.decimal()?.toFloat()?.coerceIn(0f, 250f) ?: 0f

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                if (currentMlFloat.roundToInt() == 0) "0 ml" else "${currentMlFloat.roundToInt()} ml",
                style = MaterialTheme.typography.titleMedium,
                color = if (currentMlFloat.roundToInt() > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Slider(
            value = currentMlFloat,
            onValueChange = { newValue ->
                val rounded = newValue.roundToInt()
                onChange(if (rounded == 0) "" else rounded.toString())
            },
            valueRange = 0f..250f,
            steps = 49,
            enabled = !saving,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("0 ml", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("250 ml", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TimeSliderField(
    seconds: String,
    error: String?,
    saving: Boolean,
    onChange: (String) -> Unit
) {
    val currentSecondsFloat = seconds.decimal()?.toFloat()?.coerceIn(5f, 40f) ?: 20f

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Tiempo", style = MaterialTheme.typography.bodyMedium)
            Text(
                "${currentSecondsFloat.roundToInt()} s",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = currentSecondsFloat,
            onValueChange = { newValue ->
                onChange(newValue.roundToInt().toString())
            },
            valueRange = 5f..40f,
            steps = 34,
            enabled = !saving,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("5 s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            FilterChip(
                selected = currentSecondsFloat.roundToInt() == 20,
                onClick = { onChange("20") },
                enabled = !saving,
                label = { Text("20 s") }
            )

            Text("40 s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun Field(
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
                IconButton(onClick = { onChange("") }) {
                    ClearIcon(label)
                }
            }
        } else {
            null
        }
    )
}

private const val contentHeightDp = 2000

@Preview(
    name = "Formulario - Vacío",
    group = "Formulario completo",
    showBackground = true,
    widthDp = 400,
    heightDp = contentHeightDp
)
@Composable
private fun ShotFormEmptyPreview() {
    MaterialTheme {
        ShotFormContent(
            modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(16.dp),
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

@Preview(
    name = "Formulario - Con Datos",
    group = "Formulario completo",
    showBackground = true,
    widthDp = 400,
    heightDp = contentHeightDp
)
@Composable
private fun ShotFormFilledPreview() {
    MaterialTheme {
        ShotFormContent(
            modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(16.dp),
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

@Preview(
    name = "Formulario - Guardando (Loading)",
    group = "Formulario completo",
    showBackground = true,
    widthDp = 400,
    heightDp = contentHeightDp
)
@Composable
private fun ShotFormSavingPreview() {
    MaterialTheme {
        ShotFormContent(
            modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(16.dp),
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

@Preview(
    name = "Formulario - Con Error de Guardado",
    group = "Formulario completo",
    showBackground = true,
    widthDp = 400,
    heightDp = contentHeightDp
)
@Composable
private fun ShotFormErrorPreview() {
    MaterialTheme {
        ShotFormContent(
            modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(16.dp),
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

// Use Interactive mode to exercise scrolling within a real viewport.
@Preview(name = "Formulario - Pantalla con scroll", showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun ShotFormScrollablePreview() {
    var draft by remember { mutableStateOf(mockDraft) }
    MaterialTheme {
        ShotForm(
            draft = draft,
            machines = mockMachines,
            beans = listOf(mockBean),
            cups = emptyList(),
            onChange = { draft = it },
            saving = false,
            saveError = null,
            modifier = Modifier.padding(16.dp),
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
