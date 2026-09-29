package com.barboza.bingomanager.view.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.barboza.bingomanager.controller.AppScreen
import com.barboza.bingomanager.controller.BingoController
import com.barboza.bingomanager.model.BingoTemplate
import com.barboza.bingomanager.view.components.PrimaryButton
import com.barboza.bingomanager.view.components.ScreenScaffold

private data class DeleteRequest(val title: String, val message: String, val action: () -> Unit)

@Composable
fun ManageScreen(controller: BingoController) {
    var deleteRequest by remember { mutableStateOf<DeleteRequest?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    ScreenScaffold(title = "Gestionar", onBack = { controller.navigate(AppScreen.Home) }) { scaffoldModifier ->
        Column(
            modifier = scaffoldModifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Cartones guardados", style = MaterialTheme.typography.titleLarge)
            if (controller.cards.isEmpty()) {
                EmptyMessage("Todavía no hay cartones. Agrega el primero desde el menú principal.")
            } else {
                controller.cards.sortedBy { it.name.lowercase() }.forEach { card ->
                    val templateName = controller.template(card.templateId)?.name ?: "Plantilla eliminada"
                    ManagementCard {
                        Text(card.name, style = MaterialTheme.typography.titleMedium)
                        Text(templateName, color = MaterialTheme.colorScheme.secondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { controller.navigate(AppScreen.CardEditor(cardId = card.id)) },
                                modifier = Modifier.weight(1f),
                            ) { Text("Editar") }
                            OutlinedButton(
                                onClick = {
                                    deleteRequest = DeleteRequest(
                                        "Eliminar cartón",
                                        "¿Eliminar “${card.name}” de forma permanente?",
                                    ) { controller.deleteCard(card.id) }
                                },
                                modifier = Modifier.weight(1f),
                            ) { Text("Eliminar") }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("Tipos de cartón", style = MaterialTheme.typography.titleLarge)
            controller.templates.forEach { template ->
                ManagementCard {
                    Text(template.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${template.rows} × ${template.columns} · ${controller.cards.count { it.templateId == template.id }} guardados",
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    if (template.id != BingoTemplate.CLASSIC_ID) {
                        OutlinedButton(onClick = {
                            deleteRequest = DeleteRequest(
                                "Eliminar tipo de cartón",
                                "Solo se eliminará si no tiene cartones ni modos asociados.",
                            ) {
                                message = controller.deleteTemplate(template.id) ?: "Tipo de cartón eliminado."
                            }
                        }) { Text("Eliminar tipo") }
                    }
                }
            }
            OutlinedButton(
                onClick = { controller.navigate(AppScreen.TemplateDesigner) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Crear otro tipo de cartón") }

            Spacer(Modifier.height(8.dp))
            Text("Modos personalizados", style = MaterialTheme.typography.titleLarge)
            if (controller.customModes.isEmpty()) {
                EmptyMessage("Crea una figura ganadora adaptada a cualquiera de tus tipos de cartón.")
            } else {
                controller.customModes.forEach { mode ->
                    ManagementCard {
                        Text(mode.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            controller.template(mode.templateId)?.name.orEmpty(),
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        OutlinedButton(onClick = {
                            deleteRequest = DeleteRequest(
                                "Eliminar modo",
                                "¿Eliminar el modo “${mode.name}”?",
                            ) { controller.deleteCustomMode(mode.id) }
                        }) { Text("Eliminar modo") }
                    }
                }
            }
            PrimaryButton("Crear modo personalizado", onClick = {
                controller.navigate(AppScreen.CustomModeEditor)
            })
            message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.height(24.dp))
        }
    }

    deleteRequest?.let { request ->
        AlertDialog(
            onDismissRequest = { deleteRequest = null },
            title = { Text(request.title) },
            text = { Text(request.message) },
            confirmButton = {
                TextButton(onClick = {
                    request.action()
                    deleteRequest = null
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { deleteRequest = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun ManagementCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().background(
            MaterialTheme.colorScheme.surface,
            RoundedCornerShape(18.dp),
        ).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun EmptyMessage(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().background(
            MaterialTheme.colorScheme.surfaceVariant,
            RoundedCornerShape(14.dp),
        ).padding(16.dp),
        color = MaterialTheme.colorScheme.secondary,
    )
}
