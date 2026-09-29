package com.barboza.bingomanager.view.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.barboza.bingomanager.controller.BingoController
import com.barboza.bingomanager.controller.SpeechNumberController
import com.barboza.bingomanager.model.BingoRules
import com.barboza.bingomanager.model.CellKind
import com.barboza.bingomanager.ui.theme.BingoRedSoft
import com.barboza.bingomanager.view.components.DisplayCardGrid
import com.barboza.bingomanager.view.components.SelectionDropdown

@Composable
fun GameScreen(controller: BingoController) {
    val session = controller.gameSession ?: return
    val template = controller.template(session.templateId) ?: return
    val cards = controller.gameCards()
    val winners = controller.winners()
    val winnerSignature = winners.map { it.id }.sorted().joinToString()
    var dismissedSignature by remember(session.templateId) { mutableStateOf("") }
    var confirmReset by remember { mutableStateOf(false) }
    var microphoneEnabled by remember { mutableStateOf(false) }
    var heardNumber by remember { mutableStateOf<Int?>(null) }
    var voiceStatus by remember { mutableStateOf("Micrófono apagado") }
    val context = LocalContext.current
    val voiceController = remember(controller, session.templateId) {
        SpeechNumberController(
            context = context,
            numberRange = {
                controller.gameSession?.let { it.minimumNumber..it.maximumNumber } ?: 1..75
            },
            onCandidate = { heardNumber = it },
            onStatus = { voiceStatus = it },
            onStopped = { microphoneEnabled = false },
        )
    }
    val microphonePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        microphoneEnabled = granted && voiceController.start()
        if (!granted) voiceStatus = "Permiso de micrófono denegado"
    }

    DisposableEffect(voiceController) {
        onDispose { voiceController.destroy() }
    }

    LaunchedEffect(winnerSignature) {
        if (winnerSignature.isEmpty()) dismissedSignature = ""
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 14.dp, bottom = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(session.mode.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    "${template.name} · ${session.minimumNumber}–${session.maximumNumber}",
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            OutlinedButton(onClick = { controller.finishGame() }) { Text("Terminar") }
        }

        SelectionDropdown(
            label = "Modo de juego",
            options = controller.modesFor(session.templateId),
            selected = session.mode,
            optionText = { it.name },
            onSelected = { controller.changeGameMode(it.id) },
            modifier = Modifier.padding(top = 10.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilledIconButton(
                onClick = {
                    if (microphoneEnabled) {
                        voiceController.stop()
                        microphoneEnabled = false
                        heardNumber = null
                    } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        microphoneEnabled = voiceController.start()
                    } else {
                        microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier.size(52.dp).semantics {
                    contentDescription = if (microphoneEnabled) "Apagar micrófono" else "Encender micrófono"
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (microphoneEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (microphoneEnabled) Color.White else MaterialTheme.colorScheme.onSurface,
                ),
            ) { Text("🎙", fontSize = 22.sp) }
            heardNumber?.let { candidate ->
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable {
                            controller.markCalledNumber(candidate)
                            heardNumber = null
                            voiceStatus = if (microphoneEnabled) "Confirmado; escuchando…" else "Número confirmado"
                        }
                        .semantics { contentDescription = "Confirmar número $candidate" },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(candidate.toString(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 19.sp)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(if (microphoneEnabled) "Micrófono encendido" else "Micrófono apagado", fontWeight = FontWeight.Bold)
                Text(voiceStatus, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
            }
            Box(
                modifier = Modifier.size(58.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    session.lastCalledNumber?.toString() ?: "—",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Último marcado · ${session.calledNumbers.size} en total", color = MaterialTheme.colorScheme.secondary)
            OutlinedButton(
                onClick = { confirmReset = true },
                enabled = session.calledNumbers.isNotEmpty(),
            ) { Text("Reiniciar") }
        }

        Text("Toca el número anunciado", style = MaterialTheme.typography.titleMedium)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(50.dp),
            modifier = Modifier.fillMaxWidth().weight(0.46f).padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items((session.minimumNumber..session.maximumNumber).toList(), key = { it }) { number ->
                val called = number in session.calledNumbers
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(if (called) MaterialTheme.colorScheme.primary else Color.White, CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable { controller.toggleCalledNumber(number) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        number.toString(),
                        color = if (called) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        if (winners.isNotEmpty()) {
            Text(
                "¡BINGO! ${winners.joinToString { it.name }}",
                modifier = Modifier.fillMaxWidth().background(BingoRedSoft, RoundedCornerShape(14.dp)).padding(12.dp),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            "Seguimiento de cartones",
            modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(0.54f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(cards, key = { it.id }) { card ->
                val marked = BingoRules.markedIndices(card, template, session.calledNumbers)
                val isWinner = winners.any { it.id == card.id }
                Column(
                    modifier = Modifier.fillMaxWidth().background(
                        if (isWinner) BingoRedSoft else MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(18.dp),
                    ).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(card.name, fontWeight = FontWeight.Bold)
                        Text(
                            if (isWinner) "GANADOR" else "${marked.count { template.cells[it] == CellKind.NUMBER }} marcados",
                            color = if (isWinner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    DisplayCardGrid(template, card.numbers, marked, compact = true)
                }
            }
        }
    }

    if (winnerSignature.isNotEmpty() && winnerSignature != dismissedSignature) {
        AlertDialog(
            onDismissRequest = { dismissedSignature = winnerSignature },
            title = { Text("¡Tenemos ganador!") },
            text = { Text(winners.joinToString(separator = "\n") { "• ${it.name}" }) },
            confirmButton = {
                Button(onClick = { dismissedSignature = winnerSignature }) { Text("Continuar") }
            },
        )
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reiniciar partida") },
            text = { Text("Se desmarcarán todos los números.") },
            confirmButton = {
                TextButton(onClick = {
                    controller.resetGame()
                    confirmReset = false
                }) { Text("Reiniciar") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancelar") } },
        )
    }
}
