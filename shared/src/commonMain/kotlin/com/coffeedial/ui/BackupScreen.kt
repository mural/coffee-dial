package com.coffeedial.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.coffeedial.ui.CoffeeButton as Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.coffeedial.ui.CoffeeOutlinedButton as OutlinedButton
import androidx.compose.material3.Text
import com.coffeedial.ui.CoffeeTextButton as TextButton
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
    val strings = LocalStrings.current
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
            message = strings.userMessage(error.message)
        } catch (_: Exception) {
            message = strings.backupFailed
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
        Text(strings.backupTitle, style = MaterialTheme.typography.headlineSmall)
        Text(strings.backupIntro)
        Text(strings.backupPrivacy)
        Button(
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                preview = null
                lastImportText = null
                files.finish(null)
                scope.launch { perform { files.requestExport(repository.exportBackup()) } }
            }
        ) { Text(strings.exportBackup) }
        OutlinedButton(
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                preview = null
                lastImportText = null
                message = null
                files.requestImport()
            }
        ) { Text(strings.importBackup) }
        if (busy) CircularProgressIndicator()
        files.message?.let { Text(strings.userMessage(it)) }
        message?.let { Text(it) }
        preview?.let { prepared ->
            val summary = prepared.summary
            Text(strings.reviewBeforeImport, style = MaterialTheme.typography.titleLarge)
            Text(
                strings.backupNewCounts(
                    summary.newBeans,
                    summary.newShots,
                    summary.newMachines,
                    summary.newGrinders,
                    summary.newCups,
                    summary.newRecipes,
                    summary.newDefaults
                )
            )
            if (summary.duplicates > 0) {
                Text(strings.backupDuplicates(summary.duplicates))
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
                        message = strings.backupImported(
                            result.newBeans,
                            result.newShots,
                            result.newGrinders,
                            result.newCups,
                            result.newRecipes
                        )
                    }
                }
            }) { Text(strings.mergeBackup) }

            if (!lastImportText.isNullOrBlank()) {
                OutlinedButton(enabled = !busy, modifier = Modifier.fillMaxWidth(), onClick = {
                    scope.launch {
                        perform {
                            val result = repository.replaceWithBackup(lastImportText!!)
                            preview = null
                            lastImportText = null
                            message = strings.backupReplaced(
                                result.newBeans,
                                result.newShots,
                                result.newMachines,
                                result.newGrinders,
                                result.newCups,
                                result.newRecipes
                            )
                        }
                    }
                }) { Text(strings.replaceAllBackup) }
            }

            TextButton(enabled = !busy, onClick = {
                preview = null
                lastImportText = null
            }) { Text(strings.actionCancel) }
        }
    }
}
