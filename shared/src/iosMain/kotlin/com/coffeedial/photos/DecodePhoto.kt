package com.coffeedial.photos
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
actual fun decodePhoto(bytes: ByteArray): ImageBitmap =
    org.jetbrains.skia.Image.makeFromEncoded(bytes).toComposeImageBitmap()
