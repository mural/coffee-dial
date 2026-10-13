package com.coffeedial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.coffeedial.ui.CoffeeButton as Button
import com.coffeedial.ui.CoffeeCard as Card
import androidx.compose.material3.MaterialTheme
import com.coffeedial.ui.CoffeeOutlinedButton as OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeedial.sync.RecordConflict
import com.coffeedial.sync.SyncState
import com.coffeedial.sync.conflictFields
import com.coffeedial.sync.conflictTitle

@Composable
fun ConflictScreen(
    state: SyncState.Conflict,
    busy: Boolean,
    onResolve: (RecordConflict, Boolean) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Conflictos", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Dos dispositivos cambiaron el mismo registro. Las dos copias siguen guardadas " +
                    "hasta que elijas una. La otra no se borra en silencio."
            )
        }
        items(state.conflicts, key = { "${it.kind}:${it.id}" }) { conflict ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(conflictTitle(conflict), style = MaterialTheme.typography.titleMedium)
                    conflictFields(conflict).forEach { field ->
                        Text(field.label, style = MaterialTheme.typography.labelLarge)
                        Text("Este dispositivo: ${field.local}")
                        Text("El otro dispositivo: ${field.remote}")
                    }
                    Button(
                        onClick = { onResolve(conflict, true) },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Quedarme con esta copia")
                    }
                    OutlinedButton(
                        onClick = { onResolve(conflict, false) },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Quedarme con la otra")
                    }
                }
            }
        }
    }
}
