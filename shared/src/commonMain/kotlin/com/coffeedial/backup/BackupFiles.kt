package com.coffeedial.backup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Main-thread bridge to the system document picker. It holds no Activity or controller itself. */
class BackupFiles {
    var exportAction: (() -> Unit)? = null
    var importAction: (() -> Unit)? = null
    var pendingExport: String? = null
        private set
    var importedText by mutableStateOf<String?>(null)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var busy by mutableStateOf(false)
        private set

    fun requestExport(text: String) {
        if (busy) return
        pendingExport = text
        busy = true
        message = null
        exportAction?.invoke() ?: finish("No se pudo abrir el selector de archivos.")
    }

    fun requestImport() {
        if (busy) return
        busy = true
        message = null
        importedText = null
        importAction?.invoke() ?: finish("No se pudo abrir el selector de archivos.")
    }

    fun receiveImport(text: String) {
        importedText = text
        finish(null)
    }

    fun consumeImport() {
        importedText = null
    }

    fun finish(result: String?) {
        busy = false
        pendingExport = null
        message = result
    }
}
