package com.barboza.bingomanager.view.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import com.barboza.bingomanager.controller.AppScreen
import com.barboza.bingomanager.controller.BingoController
import com.barboza.bingomanager.model.CellKind
import com.barboza.bingomanager.view.components.PatternCell
import com.barboza.bingomanager.view.components.PrimaryButton
import com.barboza.bingomanager.view.components.ScreenScaffold
import com.barboza.bingomanager.view.components.SelectionDropdown

@Composable
fun CustomModeEditorScreen(controller: BingoController) {
    var name by remember { mutableStateOf("") }
    var templateId by remember { mutableStateOf(controller.templates.first().id) }
    var selectedCells by remember { mutableStateOf(emptySet<Int>()) }
    var error by remember { mutableStateOf<String?>(null) }
    val template = controller.template(templateId) ?: controller.templates.first()

    ScreenScaffold(title = "Crear modo de juego", onBack = { controller.navigate(AppScreen.Manage) }) { scaffoldModifier ->
        Column(
            modifier = scaffoldModifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "Selecciona todas las casillas que deben estar marcadas para ganar. Los espacios libres cuentan automáticamente.",
                color = MaterialTheme.colorScheme.secondary,
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(50) },
                label = { Text("Nombre, por ejemplo: Flecha") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            SelectionDropdown(
                label = "Tipo de cartón",
                options = controller.templates,
                selected = template,
                optionText = { it.name },
                onSelected = {
                    templateId = it.id
                    selectedCells = emptySet()
                    error = null
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    selectedCells = template.cells.indices.filterTo(mutableSetOf()) {
                        template.cells[it] != CellKind.DISABLED
                    }
                }) { Text("Seleccionar todo") }
                OutlinedButton(onClick = { selectedCells = emptySet() }) { Text("Limpiar") }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.Center) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(template.rows) { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            repeat(template.columns) { column ->
                                val index = row * template.columns + column
                                val active = template.cells[index] != CellKind.DISABLED
                                PatternCell(
                                    text = if (template.cells[index] == CellKind.FREE) "★" else if (active) "#" else "",
                                    selected = index in selectedCells,
                                    enabled = active,
                                    onClick = {
                                        selectedCells = if (index in selectedCells) selectedCells - index else selectedCells + index
                                        error = null
                                    },
                                    size = 44,
                                )
                            }
                        }
                    }
                }
            }
            Text("${selectedCells.size} casillas seleccionadas", color = MaterialTheme.colorScheme.secondary)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            PrimaryButton("Guardar modo", onClick = {
                error = controller.createCustomMode(name, template.id, selectedCells)
            })
        }
    }
}
