package com.coffeedial.photos

import androidx.compose.ui.graphics.ImageBitmap
import kotlin.io.encoding.Base64
import kotlinx.serialization.Serializable

@Serializable
data class BeanPhoto(val id: String, val jpeg: String? = null) {
    fun validate() {
        require(Regex("[a-f0-9-]{36}").matches(id)) { "Foto inválida." }
        jpeg?.let {
            require(it.length <= 136536) { "La foto supera los 100 KB." }
            val bytes = Base64.decode(it)
            require(
                bytes.size in 4..102400 &&
                    bytes[0] == (-1).toByte() && bytes[1] == (-40).toByte() &&
                    bytes[bytes.lastIndex] == (-39).toByte() &&
                    bytes[bytes.lastIndex - 1] == (-1).toByte() && validDimensions(bytes)
            ) { "Foto JPEG inválida." }
        }
    }
}

expect fun decodePhoto(bytes: ByteArray): ImageBitmap

private fun validDimensions(bytes: ByteArray): Boolean {
    fun u(i: Int) = bytes[i].toInt() and 255
    var i = 2
    while (i + 8 < bytes.size) {
        if (u(i++) != 255) return false
        val marker = u(i++)
        val length = u(i) * 256 + u(i + 1)
        if (length < 2 || i + length > bytes.size) return false
        if (marker in listOf(192, 193, 194)) {
            val height = u(i + 3) * 256 + u(i + 4)
            val width = u(i + 5) * 256 + u(i + 6)
            return height in 1..640 && width in 1..640
        }
        i += length
    }
    return false
}
