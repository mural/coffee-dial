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
    var lastImportText by remember { mutableStateOf<String?>(null) }
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
            lastImportText = text
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
                lastImportText = null
                files.finish(null)
                scope.launch { perform { files.requestExport(repository.exportBackup()) } }
            }
        ) { Text("Exportar backup") }
        OutlinedButton(
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                preview = null
                lastImportText = null
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
            Text(
                "${summary.newBeans} cafés · ${summary.newShots} shots · " +
                    "${summary.newMachines} máquinas · ${summary.newCups} tazas nuevas"
            )
            if (summary.duplicates > 0) {
                Text("${summary.duplicates} shots ya existentes.")
            }

            Button(enabled = !busy, modifier = Modifier.fillMaxWidth(), onClick = {
                scope.launch {
                    perform {
                        val result = if (lastImportText != null) {
                            repository.forceImportBackup(lastImportText!!)
                        } else {
                            repository.importBackup(prepared)
                        }
                        preview = null
                        lastImportText = null
                        message = "Importación completa: ${result.newBeans} cafés, " +
                            "${result.newShots} shots y ${result.newCups} tazas procesados."
                    }
                }
            }) { Text("Combinar datos del backup") }

            if (!lastImportText.isNullOrBlank()) {
                OutlinedButton(enabled = !busy, modifier = Modifier.fillMaxWidth(), onClick = {
                    scope.launch {
                        perform {
                            val result = repository.replaceWithBackup(lastImportText!!)
                            preview = null
                            lastImportText = null
                            message = "Reemplazo completo: ${result.newBeans} cafés, " +
                                "${result.newShots} shots, ${result.newMachines} máquinas y ${result.newCups} tazas cargados desde el backup."
                        }
                    }
                }) { Text("Reemplazar todo con este backup") }
            }

            TextButton(enabled = !busy, onClick = {
                preview = null
                lastImportText = null
            }) { Text("Cancelar") }
        }
    }
}
