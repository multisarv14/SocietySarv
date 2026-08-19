package com.example.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream

/**
 * Images are stored inside the Firestore JSON documents themselves, as Base64
 * blob strings (fields ending in `Base64`), and decoded back into a Bitmap for
 * display. That keeps a document self-contained - no Cloud Storage bucket and
 * no second round trip to render a photo.
 *
 * Firestore caps a single document at 1 MB, so [fromBitmap] compresses down to
 * a sane size before encoding. Blob fields are also excluded from indexing
 * (see `firestore.indexes.json` -> fieldOverrides).
 */
object ImageBlob {

    /** Largest edge, in pixels, an image is scaled down to before encoding. */
    private const val MAX_EDGE_PX = 1024

    /** Roughly 700 KB of Base64 text, comfortably inside the 1 MB doc limit. */
    const val MAX_BLOB_CHARS = 700_000

    /** Encodes a bitmap into the Base64 JPEG string stored in Firestore. */
    fun fromBitmap(bitmap: Bitmap, quality: Int = 80): String {
        val scaled = downscale(bitmap)
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(1, 100), out)
        val encoded = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        return if (encoded.length <= MAX_BLOB_CHARS || quality <= 30) {
            encoded
        } else {
            fromBitmap(bitmap, quality - 15)
        }
    }

    /** Encodes raw bytes (for example a picked file) without re-compressing. */
    fun fromBytes(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    /** Decodes a stored blob back into a Bitmap, or null when empty/corrupt. */
    fun toBitmap(base64: String?): Bitmap? {
        if (base64.isNullOrBlank()) return null
        return runCatching {
            val clean = base64.substringAfter("base64,", base64).trim()
            val bytes = Base64.decode(clean, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    /** Decodes a stored blob straight into a Compose [ImageBitmap]. */
    fun toImageBitmap(base64: String?): ImageBitmap? = toBitmap(base64)?.asImageBitmap()

    private fun downscale(bitmap: Bitmap): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= MAX_EDGE_PX) return bitmap
        val ratio = MAX_EDGE_PX.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt().coerceAtLeast(1),
            (bitmap.height * ratio).toInt().coerceAtLeast(1),
            true
        )
    }
}

/**
 * Remembers the decoded form of a Base64 blob so a photo is only decoded when
 * the underlying string actually changes.
 */
@Composable
fun rememberBlobImage(base64: String?): ImageBitmap? =
    remember(base64) { ImageBlob.toImageBitmap(base64) }
