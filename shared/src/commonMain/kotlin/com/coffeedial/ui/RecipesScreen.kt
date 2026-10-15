package com.coffeedial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeedial.domain.RatioEdit
import com.coffeedial.domain.Recipe
import com.coffeedial.domain.RecipeAmounts
import com.coffeedial.domain.decimal
import com.coffeedial.ui.CoffeeButton as Button
import com.coffeedial.ui.CoffeeCard as Card
import com.coffeedial.ui.CoffeeTextButton as TextButton
import kotlinx.serialization.json.Json

@Composable
fun RecipesScreen(
    recipes: List<Recipe>,
    initial: String?,
    saving: Boolean,
    error: String?,
    onSave: (Recipe, () -> Unit) -> Unit,
    onDelete: (String) -> Unit,
    onPrepare: (Recipe, Boolean) -> Unit,
    onHistory: () -> Unit
) {
    val strings = LocalStrings.current
    var editing by rememberSaveable(initial) { mutableStateOf(initial) }
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    val recipe = editing?.let { Json.decodeFromString<Recipe>(it) }
    if (recipe != null) {
        RecipeEditor(recipe, saving, error, { onSave(it) { editing = null } }, onPrepare) {
            editing =
                null
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
        ) {
            item {
                Text(strings.navRecipes, style = MaterialTheme.typography.headlineLarge)
                TextButton(onClick = onHistory, enabled = !saving) {
                    Text(strings.homeViewAllShots)
                }
                error?.let {
                    Text(strings.userMessage(it), color = MaterialTheme.colorScheme.error)
                }
            }
            if (recipes.isEmpty()) item { Text(strings.homeNoShotsYet) }
            items(recipes, key = { it.id }) { item ->
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(item.name, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${item.beanName} · ${item.dose.pretty()} g -> ${item.output.pretty()} g · 1:${(item.output / item.dose).pretty()}"
                        )
                        Text("${strings.grind}: ${strings.grindLevelLabel(item.grind)}")
                        item.milkFoam?.let { Text(strings.foamLabelText(it.label(strings))) }
                        Button(onClick = {
                            onPrepare(item, true)
                        }, enabled = !saving) { Text(strings.repeatWithTimer) }
                        Row {
                            TextButton(onClick = {
                                editing = Json.encodeToString(item)
                            }, enabled = !saving) { Text(strings.viewOrAdjust) }
                            TextButton(onClick = {
                                deleting = item.id
                            }, enabled = !saving) { Text(strings.actionDelete) }
                        }
                    }
                }
            }
        }
    }
    deleting?.let { id ->
        AlertDialog(
            onDismissRequest = { if (!saving) deleting = null },
            title = {
                Text(strings.deleteRecipeQuestion)
            },
            text = { Text(strings.shotsPreservedNote) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(id)
                    deleting = null
                }, enabled = !saving) { Text(strings.actionDelete) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }, enabled = !saving) { Text(strings.actionCancel) }
            }
        )
    }
}

@Composable
private fun RecipeEditor(
    recipe: Recipe,
    saving: Boolean,
    error: String?,
    onSave: (Recipe) -> Unit,
    onPrepare: (Recipe, Boolean) -> Unit,
    onCancel: () -> Unit
) {
    val strings = LocalStrings.current
    var name by rememberSaveable(recipe.id) { mutableStateOf(recipe.name) }
    var dose by rememberSaveable(recipe.id) { mutableStateOf(recipe.dose.toString()) }
    var output by rememberSaveable(recipe.id) { mutableStateOf(recipe.output.toString()) }
    var ratio by rememberSaveable(recipe.id) {
        mutableStateOf((recipe.output / recipe.dose).toString())
    }
    var grind by rememberSaveable(recipe.id) { mutableStateOf(recipe.grind) }
    var milk by rememberSaveable(recipe.id) { mutableStateOf(recipe.milk?.toString().orEmpty()) }
    var foam by rememberSaveable(recipe.id) { mutableStateOf(recipe.milkFoam?.name) }
    var water by rememberSaveable(recipe.id) {
        mutableStateOf(recipe.extraWater?.toString().orEmpty())
    }
    var message by rememberSaveable(recipe.id) { mutableStateOf<String?>(null) }
    fun change(field: RatioEdit, text: String) {
        val current =
            RecipeAmounts(dose.decimal() ?: recipe.dose, output.decimal() ?: recipe.output)
        when (field) {
            RatioEdit.DOSE -> dose = text

            RatioEdit.OUTPUT -> output = text

            RatioEdit.RATIO ->
                ratio =
                    text
        }
        val next = text.decimal()?.let { current.change(field, it) }
        message = if (next == null) strings.recipeAmountHint else null
        if (next != null) {
            if (field != RatioEdit.DOSE) dose = next.dose.toString()
            if (field != RatioEdit.OUTPUT) output = next.output.toString()
            if (field != RatioEdit.RATIO) ratio = next.ratio.toString()
        }
    }
    fun optional(text: String): Double? = if (text.isBlank()) null else text.decimal() ?: Double.NaN
    val next = recipe.copy(
        name = name.trim(),
        dose = dose.decimal() ?: Double.NaN,
        output = output.decimal() ?: Double.NaN,
        grind = grind,
        milk = optional(milk),
        extraWater = optional(water),
        milkFoam = foam?.let(com.coffeedial.domain.MilkFoam::valueOf)
    )
    val valid =
        next.isValid() && message == null &&
            ratio.decimal()?.let { it.isFinite() && it > 0 } == true
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
    ) {
        item { Text(strings.recipeTitle, style = MaterialTheme.typography.headlineLarge) }
        item {
            CoffeeSection(strings.yourRecipe) {
                Field(strings.nameField, name, null, saving) { name = it.take(100) }
                Text(recipe.beanName, style = MaterialTheme.typography.titleMedium)
                Text(listOfNotNull(recipe.machine, recipe.grinder, recipe.cup).joinToString(" · "))
                recipe.temperature?.let { Text(strings.temperatureLine(it.pretty())) }
                Text(strings.doseKeepsRatio)
            }
        }
        item {
            CoffeeSection(strings.extractionSection) {
                Field(strings.doseFieldG, dose, null, saving, true) { change(RatioEdit.DOSE, it) }
                Field(strings.extractionRatioField, ratio, null, saving, true) {
                    change(RatioEdit.RATIO, it)
                }
                Field(strings.outputFieldG, output, null, saving, true) {
                    change(RatioEdit.OUTPUT, it)
                }
                GrindSettingField(grind, null, saving) { grind = it }
            }
        }
        item {
            CoffeeSection(strings.additionsHeading) {
                Field(strings.extraWaterOptional, water, null, saving, true) { water = it }
                Field(strings.milkOptionalMl, milk, null, saving, true) { milk = it }
                MilkFoamChoices(foam?.let(com.coffeedial.domain.MilkFoam::valueOf), !saving) {
                    foam =
                        it?.name
                }
            }
        }
        item {
            CoffeeSection(strings.saveOrBrew) {
                Text(strings.timeLoggedAgain)
                (message ?: error)?.let {
                    Text(strings.userMessage(it), color = MaterialTheme.colorScheme.error)
                }
                if (!valid &&
                    message == null
                ) {
                    Text(strings.checkRecipeFields, color = MaterialTheme.colorScheme.error)
                }
                Button(onClick = {
                    onSave(next)
                }, enabled = !saving && valid) { Text(strings.saveRecipe) }
                TextButton(onClick = {
                    onPrepare(next, true)
                }, enabled = !saving && valid) { Text(strings.myRecipesRepeat) }
                TextButton(onClick = {
                    onPrepare(next, false)
                }, enabled = !saving && valid) { Text(strings.logAShot) }
                TextButton(onClick = onCancel, enabled = !saving) { Text(strings.actionCancel) }
            }
        }
    }
}
