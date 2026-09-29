package com.barboza.bingomanager.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlin.math.max

data class NormalizedCrop(
    val left: Float = 0.04f,
    val top: Float = 0.04f,
    val right: Float = 0.96f,
    val bottom: Float = 0.96f,
)

fun loadOrientedBitmap(context: Context, uri: Uri, maximumSide: Int = 2048): Bitmap? {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sample = 1
    while (max(bounds.outWidth / sample, bounds.outHeight / sample) > maximumSide) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
    val orientation = resolver.openInputStream(uri)?.use { stream ->
        ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } ?: ExifInterface.ORIENTATION_NORMAL
    val degrees = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    if (degrees == 0f) return decoded
    return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(degrees) }, true)
        .also { if (it !== decoded) decoded.recycle() }
}

fun cropBitmap(bitmap: Bitmap, crop: NormalizedCrop): Bitmap {
    val left = (crop.left.coerceIn(0f, 0.95f) * bitmap.width).toInt()
    val top = (crop.top.coerceIn(0f, 0.95f) * bitmap.height).toInt()
    val right = (crop.right.coerceIn(crop.left + 0.02f, 1f) * bitmap.width).toInt()
    val bottom = (crop.bottom.coerceIn(crop.top + 0.02f, 1f) * bitmap.height).toInt()
    val result = Bitmap.createBitmap(bitmap, left, top, (right - left).coerceAtLeast(1), (bottom - top).coerceAtLeast(1))
    return if (result === bitmap) bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, false) else result
}
