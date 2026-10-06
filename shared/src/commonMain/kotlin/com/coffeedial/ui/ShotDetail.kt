package com.coffeedial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeedial.domain.Shot
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ShotDetail(shot: Shot, onEdit: () -> Unit, onDelete: () -> Unit, saving: Boolean) {
    var showConfirm by remember { mutableStateOf(false) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text(shot.bean.name, style = MaterialTheme.typography.headlineLarge) }
        if (shot.bean.roaster.isNotBlank()) item { Text(shot.bean.roaster) }
        if (!shot.machine.isNullOrBlank()) {
            item { Text("Máquina: ${shot.machine}") }
        }
        if (!shot.cup.isNullOrBlank()) {
            item { Text("Taza: ${shot.cup}") }
        }
        item { Text(timestamp(shot.createdAt)) }
        item { Text("1:${shot.ratio.pretty()}", style = MaterialTheme.typography.displayMedium) }
        item { Text("Ratio de extracción") }
        item {
            val total = shot.output + (shot.milk ?: 0.0) + (shot.extraWater ?: 0.0)
            Text(
                "Entrada: ${shot.dose.pretty()} g\n" +
                    "Output: ${shot.output.pretty()} g" +
                    (shot.style?.let { "\nEstilo: $it" } ?: "") +
                    (shot.extraWater?.let { "\nAgua extra: ${it.pretty()} ml" } ?: "") +
                    (shot.milk?.let { "\nLeche: ${it.pretty()} ml" } ?: "") +
                    (
                        if (shot.milk != null ||
                            shot.extraWater != null
                        ) {
                            "\nTotal: ${total.pretty()} g"
                        } else {
                            ""
                        }
                        ) +
                    "\nTiempo: ${shot.seconds.pretty()} s"
            )
        }
        item { Text("Molienda: ${shot.grind}") }
        item {
            Text("Temperatura: ${shot.temperature?.let { "${it.pretty()} °C" } ?: "Sin registrar"}")
        }
        item { CoffeeRating(shot.rating) }
        item {
            Text(shot.notes.ifBlank { "Sin notas" })
        }
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                Button(
                    onClick = onEdit,
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Editar shot")
                }
                if (showConfirm) {
                    Text("¿Querés eliminar este shot?", color = MaterialTheme.colorScheme.error)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onDelete,
                            enabled = !saving,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Sí, eliminar")
                        }
                        OutlinedButton(
                            onClick = { showConfirm = false },
                            enabled = !saving,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar")
                        }
                    }
                } else {
                    TextButton(
                        onClick = { showConfirm = true },
                        enabled = !saving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Eliminar shot", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Preview(name = "Detalle del Shot", showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun ShotDetailPreview() {
    MaterialTheme {
        ShotDetail(shot = mockShot, onEdit = {}, onDelete = {}, saving = false)
    }
}
