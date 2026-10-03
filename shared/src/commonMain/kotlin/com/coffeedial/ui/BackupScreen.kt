package com.coffeedial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeedial.backup.BackupException
import com.coffeedial.backup.BackupFiles
import com.coffeedial.data.PreparedImport
import com.coffeedial.data.ShotRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@Composable
internal fun BackupScreen(
    repository: ShotRepository,
    files: BackupFiles,
    processing: Boolean,
    setProcessing: (Boolean) -> Unit
) {
    var preview by remember { mutableStateOf<PreparedImport?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val busy = processing || files.busy

    suspend fun perform(block: suspend () -> Unit) {
        setProcessing(true)
        message = null
        try {
            block()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: BackupException) {
            message = error.message
        } catch (_: Exception) {
            message = "No pudimos completar la operación. Tus datos locales se conservan."
        } finally {
            setProcessing(false)
        }
    }

    LaunchedEffect(files) {
        snapshotFlow { files.importedText }.filterNotNull().collect { text ->
            files.consumeImport()
            preview = null
            perform { preview = repository.prepareImport(text) }
        }
    }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Backup", style = MaterialTheme.typography.headlineSmall)
        Text("Guardá una copia de tus cafés y shots, o recuperalos desde un archivo.")
        Text("El archivo incluye tus notas y no está cifrado. Elegí dónde guardarlo.")
        Button(
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                preview = null
                files.finish(null)
                scope.launch { perform { files.requestExport(repository.exportBackup()) } }
            }
        ) { Text("Exportar backup") }
        OutlinedButton(
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                preview = null
                message = null
                files.requestImport()
            }
        ) { Text("Importar backup") }
        if (busy) CircularProgressIndicator()
        files.message?.let { Text(it) }
        message?.let { Text(it) }
        preview?.let { prepared ->
            val summary = prepared.summary
            Text("Revisá antes de importar", style = MaterialTheme.typography.titleLarge)
            Text("${summary.newBeans} cafés nuevos · ${summary.newShots} shots nuevos")
            Text("${summary.duplicates} shots ya existentes: no se duplicarán.")
            if (summary.conflicts > 0) {
                Text("${summary.conflicts} registros con conflictos: se omitirán.")
                Text("Se conservan los datos actuales. No se sobrescribe ni se borra nada.")
            }
            if (summary.newBeans + summary.newShots == 0) {
                Text("No hay datos nuevos para importar.")
            } else {
                Button(enabled = !busy, onClick = {
                    scope.launch {
                        perform {
                            val result = repository.importBackup(prepared)
                            preview = null
                            message = "Importación completa: ${result.newBeans} cafés y " +
                                "${result.newShots} shots agregados."
                        }
                    }
                }) { Text("Confirmar importación de nuevos") }
            }
            TextButton(enabled = !busy, onClick = { preview = null }) { Text("Cancelar") }
        }
    }
}
