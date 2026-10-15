package com.coffeedial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
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
import com.coffeedial.domain.Grinder
import com.coffeedial.domain.Machine
import com.coffeedial.domain.ShotDraft
import com.coffeedial.domain.prepareDrinks
import com.coffeedial.domain.decimal
import com.coffeedial.ui.CoffeeButton as Button
import com.coffeedial.ui.CoffeeTextButton as TextButton
import kotlin.math.roundToInt
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ShotForm(
    draft: ShotDraft,
    machines: List<Machine>,
    beans: List<Bean>,
    cups: List<Cup>,
    grinders: List<Grinder>,
    onChange: (ShotDraft) -> Unit,
    saving: Boolean,
    saveError: String?,
    modifier: Modifier = Modifier,
    isEditing: Boolean = false,
    onOpenTimer: (() -> Unit)? = null,
    onNavigateToMachines: () -> Unit,
    onNavigateToBeans: () -> Unit,
    onNavigateToCups: () -> Unit,
    onNavigateToGrinders: () -> Unit,
    onSave: () -> Unit
) {
    var submitted by rememberSaveable { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxHeight()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 12.dp)
        ) {
            ShotFormContent(
                draft = draft,
                machines = machines,
                beans = beans,
                cups = cups,
                grinders = grinders,
                onChange = onChange,
                saving = saving,
                saveError = null,
                isEditing = isEditing,
                showSaveButton = false,
                submitted = submitted,
                onOpenTimer = onOpenTimer,
                onNavigateToMachines = onNavigateToMachines,
                onNavigateToBeans = onNavigateToBeans,
                onNavigateToCups = onNavigateToCups,
                onNavigateToGrinders = onNavigateToGrinders,
                onSave = onSave
            )
        }
        HorizontalDivider()
        SaveShotButton(
            saving = saving,
            isEditing = isEditing,
            saveError = saveError,
            showFieldError = submitted && draft.errors().isNotEmpty(),
            onClick = {
                submitted = true
                if (draft.errors().isEmpty()) onSave()
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp)
        )
    }
}

@Composable
fun ShotFormContent(
    draft: ShotDraft,
    machines: List<Machine>,
    beans: List<Bean>,
    cups: List<Cup>,
    grinders: List<Grinder>,
    onChange: (ShotDraft) -> Unit,
    saving: Boolean,
    saveError: String?,
    modifier: Modifier = Modifier,
    isEditing: Boolean = false,
    onOpenTimer: (() -> Unit)? = null,
    onNavigateToMachines: () -> Unit,
    onNavigateToBeans: () -> Unit,
    onNavigateToCups: () -> Unit,
    onNavigateToGrinders: () -> Unit,
    onSave: () -> Unit,
    showSaveButton: Boolean = true,
    submitted: Boolean = false
) {
    var localSubmitted by rememberSaveable { mutableStateOf(false) }
    val strings = LocalStrings.current
    val activeSubmitted = if (showSaveButton) localSubmitted else submitted
    val errors = if (activeSubmitted) draft.errors(strings) else emptyMap()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            if (isEditing) strings.editShotTitle else strings.newShotTitle,
            style = MaterialTheme.typography.headlineSmall
        )

        CoffeeSection(strings.beans) {
            if (beans.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        itemVerticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(strings.beans, style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = onNavigateToBeans, enabled = !saving) {
                            Text(strings.manageBeans)
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
                    Field(strings.beanNameField, draft.beanName, errors["beanName"], saving) {
                        onChange(draft.copy(beanName = it))
                    }
                }
            } else {
                Column {
                    Field(strings.beanSection, draft.beanName, errors["beanName"], saving) {
                        onChange(draft.copy(beanName = it))
                    }
                    TextButton(onClick = onNavigateToBeans, enabled = !saving) {
                        Text(strings.addSavedBean)
                    }
                }
            }

            Field(strings.roasterOptionalField, draft.roaster, null, saving) {
                onChange(draft.copy(roaster = it))
            }
        }

        CoffeeSection(strings.extractionSection, strings.extractionHint) {
            DoseSliderField(draft.dose, errors["dose"], saving) {
                onChange(draft.copy(dose = it))
            }

            OutputSliderField(draft.output, errors["output"], saving) {
                onChange(draft.copy(output = it))
            }

            onOpenTimer?.let { open ->
                TextButton(onClick = open, enabled = !saving) { Text(strings.measureWithTimer) }
            }
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                value = draft.firstDrop,
                onValueChange = { onChange(draft.copy(firstDrop = it)) },
                label = { Text(strings.firstDropOptional) },
                enabled = !saving,
                singleLine = true,
                isError = errors["firstDrop"] != null,
                supportingText = errors["firstDrop"]?.let { { Text(it) } }
            )
            TimeSliderField(draft.seconds, errors["seconds"], saving) {
                onChange(draft.copy(seconds = it))
            }

            Field(
                strings.temperatureOptional,
                draft.temperature,
                errors["temperature"],
                saving,
                true
            ) {
                onChange(draft.copy(temperature = it))
            }
        }

        CoffeeSection(strings.yourDrink, strings.drinkHint) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Field(
                    strings.styleOptional,
                    strings.knownDrinkName(draft.style),
                    errors["style"],
                    saving
                ) {
                    onChange(draft.copy(style = styleToStore(it, strings)))
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    prepareDrinks.forEach { option ->
                        val style = option.name
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
                            label = { Text(strings.knownDrinkName(style)) }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                MlSliderField(strings.addedWater, draft.extraWater, errors["extraWater"], saving) {
                    onChange(draft.copy(extraWater = it))
                }
                Text(
                    strings.waterAfterExtraction,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            MlSliderField(strings.milk, draft.milk, errors["milk"], saving) {
                onChange(draft.copy(milk = it))
            }
            MilkFoamChoices(draft.milkFoam, !saving) { onChange(draft.copy(milkFoam = it)) }
            val selectedCup = cups.firstOrNull { it.name == draft.cup }
            DrinkCupChart(
                dose = draft.dose.decimal() ?: 0.0,
                output = draft.output.decimal() ?: 0.0,
                water = draft.extraWater.decimal() ?: 0.0,
                milk = draft.milk.decimal() ?: 0.0,
                capacityCm3 = selectedCup?.capacity,
                milkFoam = draft.milkFoam,
                canUseGenericFill = true
            )
        }

        CoffeeSection(strings.equipmentAndGrind) {
            if (machines.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        itemVerticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(strings.machine, style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = onNavigateToMachines, enabled = !saving) {
                            Text(strings.manageMachines)
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    Field(strings.orTypeMachine, draft.machine, null, saving) {
                        onChange(draft.copy(machine = it))
                    }
                }
            } else {
                Column {
                    Field(strings.machineOptional, draft.machine, null, saving) {
                        onChange(draft.copy(machine = it))
                    }
                    TextButton(onClick = onNavigateToMachines, enabled = !saving) {
                        Text(strings.addSavedMachine)
                    }
                }
            }

            if (grinders.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        itemVerticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            strings.grinderOptionalField,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(onClick = onNavigateToGrinders, enabled = !saving) {
                            Text(strings.manageGrinders)
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        grinders.forEach { grinder ->
                            val selected = draft.grinderId == grinder.id ||
                                (draft.grinderId == null && draft.grinder == grinder.name)
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    onChange(
                                        if (draft.grinderId == grinder.id) {
                                            draft.copy(grinder = "", grinderId = null)
                                        } else {
                                            draft.copy(
                                                grinder = grinder.name,
                                                grinderId = grinder.id
                                            )
                                        }
                                    )
                                },
                                label = { Text(grinder.name) },
                                enabled = !saving
                            )
                        }
                    }
                    Field(strings.orTypeGrinder, draft.grinder, null, saving) {
                        onChange(
                            draft.copy(
                                grinder = it,
                                grinderId = grinders.find { item ->
                                    item.id == draft.grinderId && item.name == it
                                }?.id
                            )
                        )
                    }
                }
            } else {
                Column {
                    Field(strings.grinderOptionalField, draft.grinder, null, saving) {
                        onChange(draft.copy(grinder = it, grinderId = null))
                    }
                    TextButton(onClick = onNavigateToGrinders, enabled = !saving) {
                        Text(strings.addSavedGrinder)
                    }
                }
            }

            if (cups.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        itemVerticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(strings.cup, style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = onNavigateToCups, enabled = !saving) {
                            Text(strings.manageCups)
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
                    Field(strings.orTypeCup, draft.cup, null, saving) {
                        onChange(draft.copy(cup = it))
                    }
                }
            } else {
                Column {
                    Field(strings.cupOptional, draft.cup, null, saving) {
                        onChange(draft.copy(cup = it))
                    }
                    TextButton(onClick = onNavigateToCups, enabled = !saving) {
                        Text(strings.addSavedCup)
                    }
                }
            }

            GrindSettingField(draft.grind, errors["grind"], saving) {
                onChange(draft.copy(grind = it))
            }
        }

        CoffeeSection(strings.howItTasted) {
            OutlinedTextField(
                value = draft.notes,
                onValueChange = { onChange(draft.copy(notes = it)) },
                label = {
                    Text(strings.notesOptional)
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                trailingIcon = if (draft.notes.isNotEmpty() && !saving) {
                    {
                        IconButton(onClick = {
                            onChange(draft.copy(notes = ""))
                        }) {
                            ClearIcon(strings.notes)
                        }
                    }
                } else {
                    null
                }
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(strings.ratingQuestion)
                CoffeeRating(draft.rating, onChange = {
                    onChange(draft.copy(rating = it))
                }, enabled = !saving)
            }
        }

        TasteProfileEditor(draft.taste, enabled = !saving) {
            onChange(draft.copy(taste = it))
        }

        CoffeeSection(strings.nextAttempt) {
            DialInFormContext(draft.dialIn, enabled = !saving) { onChange(draft.copy(dialIn = it)) }
        }

        if (showSaveButton) {
            SaveShotButton(
                saving = saving,
                isEditing = isEditing,
                saveError = saveError,
                showFieldError = errors.isNotEmpty(),
                onClick = {
                    localSubmitted = true
                    if (draft.errors().isEmpty()) onSave()
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun SaveShotButton(
    saving: Boolean,
    isEditing: Boolean,
    saveError: String?,
    showFieldError: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (saveError != null) {
            Text(strings.userMessage(saveError), color = MaterialTheme.colorScheme.error)
        }
        if (showFieldError) {
            Text(
                strings.checkMarkedFields,
                color = MaterialTheme.colorScheme.error
            )
        }
        Button(onClick = onClick, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
            Text(
                when {
                    isEditing -> strings.actionSave
                    else -> strings.saveShotLabel
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
    val strings = LocalStrings.current
    val currentDoseFloat = dose.decimal()?.toFloat()?.coerceIn(10f, 40f) ?: 18f

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(strings.doseIn, style = MaterialTheme.typography.bodyMedium)
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
            Text(
                "10 g",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

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

            Text(
                "40 g",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (error != null) {
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
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
    val strings = LocalStrings.current
    val currentOutputFloat = output.decimal()?.toFloat()?.coerceIn(10f, 100f) ?: 36f

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(strings.outputField, style = MaterialTheme.typography.bodyMedium)
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
            Text(
                "10 g",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "100 g",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (error != null) {
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
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
                if (currentMlFloat.roundToInt() ==
                    0
                ) {
                    "0 ml"
                } else {
                    "${currentMlFloat.roundToInt()} ml"
                },
                style = MaterialTheme.typography.titleMedium,
                color = if (currentMlFloat.roundToInt() >
                    0
                ) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
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
            Text(
                "0 ml",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "250 ml",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (error != null) {
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
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
    val strings = LocalStrings.current
    val currentSecondsFloat = seconds.decimal()?.toFloat()?.coerceIn(5f, 40f) ?: 20f

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(strings.time, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${seconds.decimal()?.pretty() ?: seconds} s",
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
            Text(
                "5 s",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FilterChip(
                selected = currentSecondsFloat.roundToInt() == 20,
                onClick = { onChange("20") },
                enabled = !saving,
                label = { Text("20 s") }
            )

            Text(
                "40 s",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (error != null) {
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
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

private const val CONTENT_HEIGHT_DP = 5000

@Preview(
    name = "Formulario - Vacío",
    group = "Formulario completo",
    showBackground = true,
    widthDp = 400,
    heightDp = CONTENT_HEIGHT_DP
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
            grinders = mockGrinders,
            onChange = {},
            saving = false,
            saveError = null,
            onNavigateToMachines = {},
            onNavigateToBeans = {},
            onNavigateToCups = {},
            onNavigateToGrinders = {},
            onSave = {}
        )
    }
}

@Preview(
    name = "Formulario - Con Datos",
    group = "Formulario completo",
    showBackground = true,
    widthDp = 400,
    heightDp = CONTENT_HEIGHT_DP
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
            grinders = mockGrinders,
            onChange = {},
            saving = false,
            saveError = null,
            onNavigateToMachines = {},
            onNavigateToBeans = {},
            onNavigateToCups = {},
            onNavigateToGrinders = {},
            onSave = {}
        )
    }
}

@Preview(
    name = "Formulario - Guardando (Loading)",
    group = "Formulario completo",
    showBackground = true,
    widthDp = 400,
    heightDp = CONTENT_HEIGHT_DP
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
            grinders = mockGrinders,
            onChange = {},
            saving = true,
            saveError = null,
            onNavigateToMachines = {},
            onNavigateToBeans = {},
            onNavigateToCups = {},
            onNavigateToGrinders = {},
            onSave = {}
        )
    }
}

@Preview(
    name = "Formulario - Con Error de Guardado",
    group = "Formulario completo",
    showBackground = true,
    widthDp = 400,
    heightDp = CONTENT_HEIGHT_DP
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
            grinders = mockGrinders,
            onChange = {},
            saving = false,
            saveError = "No pudimos guardar el shot. Tus datos siguen acá; intentá otra vez.",
            onNavigateToMachines = {},
            onNavigateToBeans = {},
            onNavigateToCups = {},
            onNavigateToGrinders = {},
            onSave = {}
        )
    }
}

// Use Interactive mode to exercise scrolling within a real viewport.
@Preview(
    name = "Formulario - Pantalla con scroll",
    showBackground = true,
    widthDp = 400,
    heightDp = 800
)
@Composable
private fun ShotFormScrollablePreview() {
    var draft by remember { mutableStateOf(mockDraft) }
    MaterialTheme {
        ShotForm(
            draft = draft,
            machines = mockMachines,
            beans = listOf(mockBean),
            cups = emptyList(),
            grinders = mockGrinders,
            onChange = { draft = it },
            saving = false,
            saveError = null,
            modifier = Modifier.padding(16.dp),
            onNavigateToMachines = {},
            onNavigateToBeans = {},
            onNavigateToCups = {},
            onNavigateToGrinders = {},
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
