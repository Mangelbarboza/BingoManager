package com.barboza.bingomanager.view.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import com.barboza.bingomanager.data.NormalizedCrop
import kotlin.math.hypot

private enum class CropDrag { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, MOVE }

@Composable
fun CropImageEditor(
    bitmap: Bitmap,
    crop: NormalizedCrop,
    onCropChange: (NormalizedCrop) -> Unit,
) {
    val currentCrop by rememberUpdatedState(crop)
    val currentOnChange by rememberUpdatedState(onCropChange)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black),
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Foto del cartón para recortar",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        Canvas(
            modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                var drag = CropDrag.MOVE
                detectDragGestures(
                    onDragStart = { position ->
                        val rect = currentCrop
                        val corners = mapOf(
                            CropDrag.TOP_LEFT to Offset(rect.left * size.width, rect.top * size.height),
                            CropDrag.TOP_RIGHT to Offset(rect.right * size.width, rect.top * size.height),
                            CropDrag.BOTTOM_LEFT to Offset(rect.left * size.width, rect.bottom * size.height),
                            CropDrag.BOTTOM_RIGHT to Offset(rect.right * size.width, rect.bottom * size.height),
                        )
                        drag = corners.minByOrNull { (_, point) ->
                            hypot(position.x - point.x, position.y - point.y)
                        }?.takeIf { (_, point) -> hypot(position.x - point.x, position.y - point.y) < 90f }?.key
                            ?: CropDrag.MOVE
                    },
                ) { change, amount ->
                    change.consume()
                    val rect = currentCrop
                    val dx = amount.x / size.width
                    val dy = amount.y / size.height
                    val minimum = 0.08f
                    val updated = when (drag) {
                        CropDrag.TOP_LEFT -> rect.copy(
                            left = (rect.left + dx).coerceIn(0f, rect.right - minimum),
                            top = (rect.top + dy).coerceIn(0f, rect.bottom - minimum),
                        )
                        CropDrag.TOP_RIGHT -> rect.copy(
                            right = (rect.right + dx).coerceIn(rect.left + minimum, 1f),
                            top = (rect.top + dy).coerceIn(0f, rect.bottom - minimum),
                        )
                        CropDrag.BOTTOM_LEFT -> rect.copy(
                            left = (rect.left + dx).coerceIn(0f, rect.right - minimum),
                            bottom = (rect.bottom + dy).coerceIn(rect.top + minimum, 1f),
                        )
                        CropDrag.BOTTOM_RIGHT -> rect.copy(
                            right = (rect.right + dx).coerceIn(rect.left + minimum, 1f),
                            bottom = (rect.bottom + dy).coerceIn(rect.top + minimum, 1f),
                        )
                        CropDrag.MOVE -> {
                            val width = rect.right - rect.left
                            val height = rect.bottom - rect.top
                            val left = (rect.left + dx).coerceIn(0f, 1f - width)
                            val top = (rect.top + dy).coerceIn(0f, 1f - height)
                            NormalizedCrop(left, top, left + width, top + height)
                        }
                    }
                    currentOnChange(updated)
                }
            },
        ) {
            val left = crop.left * size.width
            val top = crop.top * size.height
            val right = crop.right * size.width
            val bottom = crop.bottom * size.height
            val shade = Color.Black.copy(alpha = 0.58f)
            drawRect(shade, size = Size(size.width, top))
            drawRect(shade, topLeft = Offset(0f, bottom), size = Size(size.width, size.height - bottom))
            drawRect(shade, topLeft = Offset(0f, top), size = Size(left, bottom - top))
            drawRect(shade, topLeft = Offset(right, top), size = Size(size.width - right, bottom - top))
            drawRect(
                Color.White,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                style = Stroke(width = 4f),
            )
            listOf(
                Offset(left, top), Offset(right, top), Offset(left, bottom), Offset(right, bottom),
            ).forEach { point ->
                drawCircle(Color.White, radius = 13f, center = point)
                drawCircle(Color(0xFFA63D40), radius = 7f, center = point)
            }
        }
    }
}
