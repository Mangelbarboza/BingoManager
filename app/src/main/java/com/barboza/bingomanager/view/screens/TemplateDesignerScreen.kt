package com.barboza.bingomanager.view.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.barboza.bingomanager.controller.AppScreen
import com.barboza.bingomanager.controller.BingoController
import com.barboza.bingomanager.model.CellKind
import com.barboza.bingomanager.view.components.PatternCell
import com.barboza.bingomanager.view.components.PrimaryButton
import com.barboza.bingomanager.view.components.ScreenScaffold

@Composable
fun TemplateDesignerScreen(controller: BingoController) {
    var name by remember { mutableStateOf("") }
    var rowsText by remember { mutableStateOf("5") }
    var columnsText by remember { mutableStateOf("5") }
    var rows by remember { mutableStateOf(5) }
    var columns by remember { mutableStateOf(5) }
    var cells by remember {
        mutableStateOf(List(25) { index -> if (index == 12) CellKind.FREE else CellKind.NUMBER })
    }
    var error by remember { mutableStateOf<String?>(null) }

    fun applySize() {
        val newRows = rowsText.toIntOrNull()
        val newColumns = columnsText.toIntOrNull()
        if (newRows == null || newColumns == null || newRows !in 1..20 || newColumns !in 1..20) {
            error = "Usa dimensiones entre 1 y 20."
            return
        }
        rows = newRows
        columns = newColumns
        cells = List(rows * columns) { CellKind.DISABLED }
        error = null
    }

    ScreenScaffold(
        title = "Diseñar tipo de cartón",
        onBack = { controller.navigate(AppScreen.CardEditor()) },
    ) { scaffoldModifier ->
        Column(
            modifier = scaffoldModifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "Dibuja la forma tocando las casillas. Cada toque cambia entre número, espacio libre y desactivada.",
                color = MaterialTheme.colorScheme.secondary,
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(50) },
                label = { Text("Nombre del tipo, por ejemplo: Bingo pequeño") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = rowsText,
                    onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) rowsText = it },
                    label = { Text("Filas") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = columnsText,
                    onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) columnsText = it },
                    label = { Text("Columnas") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                OutlinedButton(onClick = ::applySize, modifier = Modifier.padding(top = 8.dp)) { Text("Aplicar") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { cells = List(rows * columns) { CellKind.NUMBER } }) { Text("Rellenar") }
                OutlinedButton(onClick = { cells = List(rows * columns) { CellKind.DISABLED } }) { Text("Limpiar") }
            }
            Text("■ Número     ★ Libre     · Desactivada", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.Center) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(rows) { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(columns) { column ->
                                val index = row * columns + column
                                val kind = cells[index]
                                PatternCell(
                                    text = when (kind) {
                                        CellKind.NUMBER -> "#"
                                        CellKind.FREE -> "★"
                                        CellKind.DISABLED -> "·"
                                    },
                                    selected = kind != CellKind.DISABLED,
                                    onClick = {
                                        cells = cells.toMutableList().also {
                                            it[index] = when (kind) {
                                                CellKind.DISABLED -> CellKind.NUMBER
                                                CellKind.NUMBER -> CellKind.FREE
                                                CellKind.FREE -> CellKind.DISABLED
                                            }
                                        }
                                    },
                                    size = 38,
                                )
                            }
                        }
                    }
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            PrimaryButton("Guardar tipo y llenar cartón", onClick = {
                error = controller.createTemplate(name, rows, columns, cells)
            })
        }
    }
}
