package com.coffeedial

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.coffeedial.backup.BackupFiles
import com.coffeedial.backup.BackupFormat
import java.io.ByteArrayOutputStream
import kotlin.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BackupPickers(files: BackupFiles) {
    val resolver = LocalContext.current.contentResolver
    val scope = rememberCoroutineScope()
    val exporter =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            val text = files.pendingExport
            if (uri == null) {
                files.finish(null)
            } else if (text == null) {
                files.finish("La exportación se interrumpió. Volvé a intentarlo.")
            } else {
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            requireNotNull(resolver.openOutputStream(uri, "wt")).use {
                                it.write(text.encodeToByteArray())
                            }
                        }
                        files.finish("Backup exportado.")
                    } catch (cancelled: CancellationException) {
                        files.finish("La exportación se interrumpió. Volvé a intentarlo.")
                        throw cancelled
                    } catch (_: Exception) {
                        files.finish("No se pudo guardar el archivo. Volvé a exportarlo.")
                    }
                }
            }
        }
    val importer =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) {
                files.finish(null)
            } else {
                scope.launch {
                    try {
                        val text = withContext(Dispatchers.IO) {
                            requireNotNull(resolver.openInputStream(uri)).use { input ->
                                val output = ByteArrayOutputStream()
                                val buffer = ByteArray(8192)
                                while (true) {
                                    val count = input.read(buffer)
                                    if (count < 0) break
                                    require(output.size() + count <= BackupFormat.MAX_BYTES)
                                    output.write(buffer, 0, count)
                                }
                                output.toByteArray().decodeToString(throwOnInvalidSequence = true)
                            }
                        }
                        files.receiveImport(text)
                    } catch (cancelled: CancellationException) {
                        files.finish("La lectura se interrumpió. Volvé a seleccionar el archivo.")
                        throw cancelled
                    } catch (_: Exception) {
                        files.finish(
                            "No se pudo leer el archivo. Elegí un backup UTF-8 de hasta 10 MB."
                        )
                    }
                }
            }
        }
    DisposableEffect(files, exporter, importer) {
        files.exportAction = {
            val date = Clock.System.now().toString().replace(':', '-')
            exporter.launch("coffee-dial-$date.json")
        }
        files.importAction =
            {
                importer.launch(
                    arrayOf("application/json", "text/plain", "application/octet-stream")
                )
            }
        onDispose {
            files.exportAction = null
            files.importAction = null
        }
    }
}
