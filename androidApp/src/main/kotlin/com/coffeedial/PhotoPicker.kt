package com.coffeedial

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.media.ExifInterface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.coffeedial.photos.PhotoPicker
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PhotoPickerHost(picker: PhotoPicker) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) {
            picker.finish("", null)
        } else {
            scope.launch {
                try {
                    val jpeg = withContext(Dispatchers.IO) {
                        val decoded = if (android.os.Build.VERSION.SDK_INT >=
                            28
                        ) {
                            ImageDecoder.decodeBitmap(
                                ImageDecoder.createSource(context.contentResolver, uri)
                            ) {
                                    decoder,
                                    info,
                                    _
                                ->
                                val scale = minOf(
                                    1.0,
                                    640.0 / maxOf(info.size.width, info.size.height)
                                )
                                decoder.setTargetSize(
                                    maxOf(1, (info.size.width * scale).toInt()),
                                    maxOf(
                                        1,
                                        (
                                            info.size.height *
                                                scale
                                            ).toInt()
                                    )
                                )
                                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                            }
                        } else {
                            val options = BitmapFactory.Options().apply {
                                inJustDecodeBounds =
                                    true
                            }
                            context.contentResolver.openInputStream(uri).use {
                                BitmapFactory.decodeStream(it, null, options)
                            }
                            require(options.outWidth > 0 && options.outHeight > 0)
                            options.inSampleSize = 1
                            while (maxOf(
                                    options.outWidth,
                                    options.outHeight
                                ) / options.inSampleSize >
                                1280
                            ) {
                                options.inSampleSize *= 2
                            }
                            options.inJustDecodeBounds = false
                            val original =
                                context.contentResolver.openInputStream(uri).use {
                                    BitmapFactory.decodeStream(it, null, options)
                                }
                                    ?: error("Invalid image")
                            val orientation = context.contentResolver.openInputStream(uri).use {
                                android.media.ExifInterface(
                                    requireNotNull(it)
                                ).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)
                            }
                            val matrix = android.graphics.Matrix().apply {
                                when (orientation) {
                                    2 -> setScale(-1f, 1f)

                                    3 -> setRotate(180f)

                                    4 -> setScale(1f, -1f)

                                    5 -> {
                                        setRotate(90f)
                                        postScale(-1f, 1f)
                                    }

                                    6 -> setRotate(90f)

                                    7 -> {
                                        setRotate(270f)
                                        postScale(-1f, 1f)
                                    }

                                    8 -> setRotate(270f)
                                }
                                val scale = minOf(1f, 640f / maxOf(original.width, original.height))
                                postScale(scale, scale)
                            }
                            val resized = Bitmap.createBitmap(
                                original,
                                0,
                                0,
                                original.width,
                                original.height,
                                matrix,
                                true
                            )
                            if (resized !== original) original.recycle()
                            resized
                        }
                        val bitmap = Bitmap.createBitmap(
                            decoded.width,
                            decoded.height,
                            Bitmap.Config.ARGB_8888
                        )
                        Canvas(bitmap).apply {
                            drawColor(Color.WHITE)
                            drawBitmap(decoded, 0f, 0f, null)
                        }
                        try {
                            var result: ByteArray? = null
                            for (quality in listOf(75, 60, 45, 30)) {
                                val out = ByteArrayOutputStream()
                                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                                if (out.size() <= 102400) {
                                    result = out.toByteArray()
                                    break
                                }
                            }
                            android.util.Base64.encodeToString(
                                requireNotNull(result),
                                android.util.Base64.NO_WRAP
                            )
                        } finally {
                            bitmap.recycle()
                            decoded.recycle()
                        }
                    }
                    picker.finish(jpeg, null)
                } catch (
                    _: Exception
                ) {
                    picker.finish("", "No se pudo preparar la foto. Elegí otra imagen.")
                }
            }
        }
    }
    DisposableEffect(picker, launcher) {
        picker.action = { launcher.launch("image/*") }
        onDispose { picker.action = null }
    }
}
