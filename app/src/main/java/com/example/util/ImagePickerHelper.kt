package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImagePickerHelper {
    fun uriToCompressedBase64(context: Context, uri: Uri, maxDim: Int = 420, quality: Int = 55): String? {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            val original = input.use { BitmapFactory.decodeStream(it) } ?: return null
            val scale = max(original.width, original.height).toFloat() / maxDim.toFloat()
            val scaled = if (scale > 1f) {
                Bitmap.createScaledBitmap(
                    original,
                    (original.width / scale).toInt().coerceAtLeast(1),
                    (original.height / scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                original
            }
            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            val bytes = baos.toByteArray()
            if (bytes.size > 150_000) {
                baos.reset()
                scaled.compress(Bitmap.CompressFormat.JPEG, 35, baos)
            }
            Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }

    fun base64ToImageBitmap(base64: String?): ImageBitmap? {
        if (base64.isNullOrBlank()) return null
        return try {
            val decoded = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decoded, 0, decoded.size)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
}
