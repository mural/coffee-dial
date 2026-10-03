@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.coffeedial

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.coffeedial.auth.InMemoryAuthRepository
import com.coffeedial.backup.BackupFiles
import com.coffeedial.data.SnapshotRepository
import com.coffeedial.data.SnapshotStore
import com.coffeedial.ui.App
import kotlin.js.JsString
import kotlin.js.Promise
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch

private external object CoffeeBrowser {
    fun read(): Promise<JsString>
    fun write(expected: String, next: String): Promise<JsString>
    fun download(text: String)
    fun pick(done: (String, String) -> Unit)
    fun ready()
    fun failed()
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    MainScope().launch {
        try {
            val repository = SnapshotRepository.open(object : SnapshotStore {
                override suspend fun read(): String? =
                    CoffeeBrowser.read().await().toString().ifEmpty {
                        null
                    }
                override suspend fun compareAndSet(expected: String?, next: String): Boolean =
                    CoffeeBrowser.write(expected ?: "", next).await().toString() == "ok"
            })
            val files = BackupFiles()
            files.exportAction = {
                try {
                    CoffeeBrowser.download(requireNotNull(files.pendingExport))
                    files.finish("Backup preparado para descargar.")
                } catch (_: Exception) {
                    files.finish("No se pudo descargar el backup.")
                }
            }
            files.importAction = {
                CoffeeBrowser.pick { text, error ->
                    when {
                        error.isNotEmpty() -> files.finish(error)
                        text.isEmpty() -> files.finish(null)
                        else -> files.receiveImport(text)
                    }
                }
            }
            ComposeViewport("app") {
                App(repository, files, InMemoryAuthRepository(), localWeb = true)
            }
            CoffeeBrowser.ready()
        } catch (_: Exception) {
            CoffeeBrowser.failed()
        }
    }
}
