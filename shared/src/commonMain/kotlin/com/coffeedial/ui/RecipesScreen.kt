package com.coffeedial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    var editing by rememberSaveable(initial) { mutableStateOf(initial) }
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    val recipe = editing?.let { Json.decodeFromString<Recipe>(it) }
    if (recipe != null) {
        RecipeEditor(recipe, saving, error, { onSave(it) { editing = null } }, onPrepare) {
            editing =
                null
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Mis recetas", style = MaterialTheme.typography.headlineLarge)
                Text("Guardá la receta de un shot desde su detalle y repetila cuando quieras.")
                TextButton(onClick = onHistory, enabled = !saving) {
                    Text("Elegir un shot del historial")
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            if (recipes.isEmpty()) item { Text("Todavía no tenés recetas guardadas.") }
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
                        Text("Molienda: ${item.grind}")
                        item.milkFoam?.let { Text("Espuma: ${it.label}") }
                        Button(onClick = {
                            onPrepare(item, true)
                        }, enabled = !saving) { Text("Repetir con timer") }
                        Row {
                            TextButton(onClick = {
                                editing = Json.encodeToString(item)
                            }, enabled = !saving) { Text("Ver / ajustar") }
                            TextButton(onClick = {
                                deleting = item.id
                            }, enabled = !saving) { Text("Eliminar") }
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
                Text("¿Eliminar receta?")
            },
            text = { Text("Los shots registrados se conservan.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(id)
                    deleting = null
                }, enabled = !saving) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }, enabled = !saving) { Text("Cancelar") }
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
        message = if (next == null) "Usá dosis de 0,1 a 100 g y output de 0,1 a 1000 g." else null
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
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Receta", style = MaterialTheme.typography.headlineLarge) }
        item { Field("Nombre", name, null, saving) { name = it.take(100) } }
        item {
            Text(recipe.beanName, style = MaterialTheme.typography.titleMedium)
            Text(listOfNotNull(recipe.machine, recipe.grinder, recipe.cup).joinToString(" · "))
            recipe.temperature?.let { Text("Temperatura: ${it.pretty()} °C") }
            Text("Al cambiar dosis se conserva el ratio; al cambiar output se recalcula el ratio.")
        }
        item { Field("Dosis · g", dose, null, saving, true) { change(RatioEdit.DOSE, it) } }
        item {
            Field("Ratio de extracción · 1:", ratio, null, saving, true) {
                change(RatioEdit.RATIO, it)
            }
        }
        item { Field("Output · g", output, null, saving, true) { change(RatioEdit.OUTPUT, it) } }
        item { GrindSettingField(grind, null, saving) { grind = it } }
        item {
            MilkFoamChoices(foam?.let(com.coffeedial.domain.MilkFoam::valueOf), !saving) {
                foam =
                    it?.name
            }
        }
        item { Text("Agregados independientes del ratio de extracción") }
        item { Field("Agua extra · ml (opcional)", water, null, saving, true) { water = it } }
        item { Field("Leche · ml (opcional)", milk, null, saving, true) { milk = it } }
        item {
            Text("El tiempo, la primera gota y el sabor se registran de nuevo al preparar.")
            (message ?: error)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (!valid &&
                message == null
            ) {
                Text(
                    "Revisá nombre, molienda y cantidades.",
                    color = MaterialTheme.colorScheme.error
                )
            }
            Button(onClick = {
                onSave(next)
            }, enabled = !saving && valid) { Text("Guardar receta") }
            TextButton(onClick = {
                onPrepare(next, true)
            }, enabled = !saving && valid) { Text("Preparar con estos valores") }
            TextButton(onClick = {
                onPrepare(next, false)
            }, enabled = !saving && valid) { Text("Registrar sin timer") }
            TextButton(onClick = onCancel, enabled = !saving) { Text("Cancelar") }
        }
    }
}
