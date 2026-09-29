package com.barboza.bingomanager.view.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.barboza.bingomanager.controller.AppScreen
import com.barboza.bingomanager.controller.BingoController
import com.barboza.bingomanager.data.NormalizedCrop
import com.barboza.bingomanager.data.cropBitmap
import com.barboza.bingomanager.data.loadOrientedBitmap
import com.barboza.bingomanager.model.CellKind
import com.barboza.bingomanager.view.components.CropImageEditor
import com.barboza.bingomanager.view.components.PrimaryButton
import com.barboza.bingomanager.view.components.ScreenScaffold
import com.barboza.bingomanager.view.components.SelectionDropdown
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File

@Composable
fun PhotoImportScreen(controller: BingoController) {
    val context = LocalContext.current
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var crop by remember { mutableStateOf(NormalizedCrop()) }
    var detectedNumbers by remember { mutableStateOf<List<Int>>(emptyList()) }
    var isReading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Toma una foto del cartón para comenzar.") }
    var templateId by remember { mutableStateOf(controller.templates.first().id) }
    val template = controller.template(templateId) ?: controller.templates.first()

    DisposableEffect(recognizer) { onDispose { recognizer.close() } }
    DisposableEffect(bitmap) {
        val currentBitmap = bitmap
        onDispose { if (currentBitmap?.isRecycled == false) currentBitmap.recycle() }
    }

    fun preparePhoto(uri: Uri) {
        isReading = true
        detectedNumbers = emptyList()
        status = "Preparando la foto…"
        runCatching { loadOrientedBitmap(context, uri) }
            .onSuccess { loaded ->
                bitmap = loaded
                crop = NormalizedCrop()
                status = if (loaded == null) {
                    "No se pudo abrir la imagen."
                } else {
                    "Mueve el marco y sus esquinas hasta dejar visibles solamente los números."
                }
                isReading = false
            }
            .onFailure {
                status = "No se pudo abrir la imagen."
                isReading = false
            }
    }

    fun processCrop() {
        val source = bitmap ?: return
        isReading = true
        detectedNumbers = emptyList()
        status = "Buscando números dentro del recorte…"
        val cropped = cropBitmap(source, crop)
        recognizer.process(InputImage.fromBitmap(cropped, 0))
            .addOnSuccessListener { result ->
                detectedNumbers = result.textBlocks
                    .flatMap { it.lines }
                    .sortedWith(compareBy({ it.boundingBox?.centerY() ?: 0 }, { it.boundingBox?.left ?: 0 }))
                    .flatMap { line -> Regex("\\d{1,4}").findAll(line.text).map { it.value.toInt() }.toList() }
                status = if (detectedNumbers.isEmpty()) {
                    "No se detectaron números. Ajusta el recorte o toma otra foto con mejor luz."
                } else {
                    "Se detectaron ${detectedNumbers.size} números. Revísalos antes de guardar."
                }
                cropped.recycle()
                isReading = false
            }
            .addOnFailureListener {
                cropped.recycle()
                status = "No se pudo analizar el recorte. Inténtalo de nuevo."
                isReading = false
            }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) pendingCameraUri?.let(::preparePhoto) else status = "No se tomó ninguna foto."
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(::preparePhoto)
    }

    fun openCamera() {
        val directory = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(directory, "bingo_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingCameraUri = uri
        cameraLauncher.launch(uri)
    }

    ScreenScaffold(title = "Agregar por foto", onBack = { controller.navigate(AppScreen.Home) }) { scaffoldModifier ->
        Column(
            modifier = scaffoldModifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "La foto y el reconocimiento se procesan localmente en el teléfono.",
                color = MaterialTheme.colorScheme.secondary,
            )
            SelectionDropdown(
                label = "Tipo del cartón fotografiado",
                options = controller.templates,
                selected = template,
                optionText = { it.name },
                onSelected = { templateId = it.id },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton(
                    text = "Tomar foto",
                    onClick = ::openCamera,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f),
                ) { Text("Galería") }
            }
            if (isReading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            Text(status)
            bitmap?.let { photo ->
                Text("Recorta solo la cuadrícula de números", style = MaterialTheme.typography.titleMedium)
                CropImageEditor(photo, crop) { crop = it }
                PrimaryButton(
                    text = "Procesar este recorte",
                    enabled = !isReading,
                    onClick = ::processCrop,
                )
            }
            if (detectedNumbers.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(16.dp),
                    ).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("Números detectados", style = MaterialTheme.typography.titleMedium)
                    Text(detectedNumbers.joinToString("  ·  "))
                }
                PrimaryButton(
                    text = "Revisar y completar cartón",
                    onClick = {
                        var sourceIndex = 0
                        val mapped = List(template.cells.size) { index ->
                            if (template.cells[index] == CellKind.NUMBER) detectedNumbers.getOrNull(sourceIndex++) else null
                        }
                        controller.navigate(AppScreen.CardEditor(templateId = template.id, importedNumbers = mapped))
                    },
                )
            }
            Text(
                "Consejo: toma la foto de frente, con buena luz, y excluye títulos, precios u otros textos del recorte.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}
