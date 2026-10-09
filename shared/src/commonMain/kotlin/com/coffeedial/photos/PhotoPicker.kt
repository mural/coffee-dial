package com.coffeedial.photos

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.uuid.Uuid

class PhotoPicker {
    var action: (() -> Unit)? = null
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    private var completion: ((BeanPhoto) -> Unit)? = null
    fun pick(done: (BeanPhoto) -> Unit) {
        if (busy) return
        busy = true
        error = null
        completion = done
        action?.invoke() ?: finish("", "No se pudo abrir el selector de fotos.")
    }
    fun finish(jpeg: String, message: String?) {
        val done = completion
        completion = null
        busy = false
        error = message
        if (jpeg.isNotEmpty()) {
            val photo = BeanPhoto(Uuid.random().toString(), jpeg)
            try {
                photo.validate()
                done?.invoke(photo)
            } catch (
                _: Exception
            ) {
                error = "No se pudo preparar la foto. Elegí otra imagen."
            }
        }
    }
    fun cancel() {
        completion = null
    }
}
