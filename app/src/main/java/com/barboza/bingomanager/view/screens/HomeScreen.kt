package com.barboza.bingomanager.view.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.barboza.bingomanager.R
import com.barboza.bingomanager.controller.AppScreen
import com.barboza.bingomanager.controller.BingoController
import com.barboza.bingomanager.model.BingoTemplate
import com.barboza.bingomanager.view.components.PrimaryButton
import com.barboza.bingomanager.view.components.SelectionDropdown

@Composable
fun HomeScreen(controller: BingoController) {
    val initialTemplate = controller.cards.firstOrNull()?.templateId ?: BingoTemplate.CLASSIC_ID
    var templateId by remember { mutableStateOf(initialTemplate) }
    val modes = remember(templateId) { controller.modesFor(templateId) }
    var modeId by remember(templateId) { mutableStateOf(modes.firstOrNull()?.id ?: "") }
    var minimumText by remember { mutableStateOf(controller.lastMinimumNumber.toString()) }
    var maximumText by remember { mutableStateOf(controller.lastMaximumNumber.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    val template = controller.template(templateId) ?: controller.templates.firstOrNull() ?: BingoTemplate.classic()
    val selectedMode = modes.firstOrNull { it.id == modeId } ?: modes.firstOrNull()
    val cardsOfType = controller.cards.count { it.templateId == template.id }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 20.dp, bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.bingo_manager_logo),
            contentDescription = "Logo de BingoManager",
            modifier = Modifier.fillMaxWidth().height(145.dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.height(26.dp))

        PrimaryButton("＋  Agregar cartón", onClick = {
            controller.navigate(AppScreen.CardEditor(templateId = template.id))
        })
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = { controller.navigate(AppScreen.PhotoImport) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            contentPadding = ButtonDefaults.ContentPadding,
        ) { Text("▣  Agregar cartón por foto", fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = { controller.navigate(AppScreen.Manage) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
        ) { Text("Gestionar cartones y modos", fontWeight = FontWeight.Bold) }

        Spacer(Modifier.height(26.dp))
        Column(
            modifier = Modifier.fillMaxWidth().background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(22.dp),
            ).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Configurar partida", style = MaterialTheme.typography.titleLarge)
            SelectionDropdown(
                label = "Tipo de cartón",
                options = controller.templates,
                selected = template,
                optionText = { it.name },
                onSelected = { selected ->
                    templateId = selected.id
                    modeId = controller.modesFor(selected.id).first().id
                    error = null
                },
            )
            Text(
                "$cardsOfType cartón${if (cardsOfType == 1) "" else "es"} disponible${if (cardsOfType == 1) "" else "s"}",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodyMedium,
            )
            SelectionDropdown(
                label = "Modo de juego",
                options = modes,
                selected = selectedMode,
                optionText = { it.name },
                onSelected = { modeId = it.id; error = null },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = minimumText,
                    onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) minimumText = it },
                    label = { Text("Desde") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = maximumText,
                    onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) maximumText = it },
                    label = { Text("Hasta") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            }
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            PrimaryButton("Iniciar juego", onClick = {
                if (selectedMode != null) {
                    error = controller.startGame(
                        templateId = template.id,
                        modeId = selectedMode.id,
                        minimum = minimumText.toIntOrNull() ?: 0,
                        maximum = maximumText.toIntOrNull() ?: 0,
                    )
                } else {
                    error = "Selecciona un modo de juego."
                }
            })
        }
        Spacer(Modifier.size(24.dp))
    }
}
