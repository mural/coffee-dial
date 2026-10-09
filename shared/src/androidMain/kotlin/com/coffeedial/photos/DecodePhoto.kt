package com.coffeedial.photos
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
actual fun decodePhoto(bytes: ByteArray): ImageBitmap =
    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size).asImageBitmap()
