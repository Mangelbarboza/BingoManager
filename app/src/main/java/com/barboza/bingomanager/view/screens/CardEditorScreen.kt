package com.barboza.bingomanager.view.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilledTonalIconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.barboza.bingomanager.controller.AppScreen
import com.barboza.bingomanager.controller.BingoController
import com.barboza.bingomanager.model.BingoCard
import com.barboza.bingomanager.model.CellKind
import com.barboza.bingomanager.model.BingoTemplate
import com.barboza.bingomanager.view.components.EditableCardGrid
import com.barboza.bingomanager.view.components.PrimaryButton
import com.barboza.bingomanager.view.components.ScreenScaffold
import com.barboza.bingomanager.view.components.SelectionDropdown
import java.util.UUID

@Composable
fun CardEditorScreen(controller: BingoController, screen: AppScreen.CardEditor) {
    val existing = controller.card(screen.cardId)
    val initialTemplateId = existing?.templateId ?: screen.templateId ?: controller.templates.first().id
    var templateId by remember(screen) { mutableStateOf(initialTemplateId) }
    val template = controller.template(templateId) ?: controller.templates.first()
    var name by remember(screen) {
        mutableStateOf(existing?.name ?: "Cartón ${controller.cards.size + 1}")
    }
    fun initialValues(): List<String> {
        val imported = screen.importedNumbers
        return List(template.cells.size) { index ->
            when {
                template.cells[index] != CellKind.NUMBER -> ""
                existing != null -> existing.numbers.getOrNull(index)?.toString().orEmpty()
                imported != null -> imported.getOrNull(index)?.toString().orEmpty()
                else -> ""
            }
        }
    }
    var values by remember(screen, templateId) { mutableStateOf(initialValues()) }
    var error by remember(screen) { mutableStateOf<String?>(null) }

    fun randomValues(): List<String> {
        if (template.id == BingoTemplate.CLASSIC_ID) {
            val byColumn = List(template.columns) { column ->
                (column * 15 + 1..column * 15 + 15).shuffled()
            }
            return List(template.cells.size) { index ->
                if (template.cells[index] == CellKind.NUMBER) {
                    val column = index % template.columns
                    val row = index / template.columns
                    byColumn[column][row].toString()
                } else ""
            }
        }
        val numericCount = template.cells.count { it == CellKind.NUMBER }
        val shuffled = (1..maxOf(75, numericCount * 2)).shuffled()
        var cursor = 0
        return List(template.cells.size) { index ->
            if (template.cells[index] == CellKind.NUMBER) shuffled[cursor++].toString() else ""
        }
    }

    ScreenScaffold(
        title = if (existing == null) "Agregar cartón" else "Editar cartón",
        onBack = { controller.navigate(if (existing == null) AppScreen.Home else AppScreen.Manage) },
    ) { scaffoldModifier ->
        Column(
            modifier = scaffoldModifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Completa los números tal como aparecen en el cartón físico.",
                color = MaterialTheme.colorScheme.secondary,
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(50) },
                label = { Text("Nombre del cartón") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            if (existing == null) {
                SelectionDropdown(
                    label = "Tipo de cartón",
                    options = controller.templates,
                    selected = template,
                    optionText = { it.name },
                    onSelected = { selected ->
                        templateId = selected.id
                        error = null
                    },
                )
                OutlinedButton(
                    onClick = { controller.navigate(AppScreen.TemplateDesigner) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Crear cartón de diferente tamaño o forma") }
            } else {
                Text("Tipo: ${template.name}", style = MaterialTheme.typography.titleMedium)
            }
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Números", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                FilledTonalIconButton(
                    onClick = {
                        values = randomValues()
                        error = null
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .semantics { contentDescription = "Llenar números aleatoriamente" },
                ) { Text("⤨") }
            }
            Text(
                "Las casillas LIBRE se marcan automáticamente.",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodyMedium,
            )
            EditableCardGrid(template, values) { index, value ->
                values = values.toMutableList().also { it[index] = value }
                error = null
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            PrimaryButton(if (existing == null) "Guardar cartón" else "Guardar cambios", onClick = {
                val numbers = List(template.cells.size) { index ->
                    if (template.cells[index] == CellKind.NUMBER) values.getOrNull(index)?.toIntOrNull() else null
                }
                error = controller.saveCard(
                    BingoCard(
                        id = existing?.id ?: UUID.randomUUID().toString(),
                        name = name,
                        templateId = template.id,
                        numbers = numbers,
                    ),
                )
            })
            Spacer(Modifier.height(24.dp))
        }
    }
}
