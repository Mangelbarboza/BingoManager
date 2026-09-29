package com.barboza.bingomanager.view

import androidx.activity.compose.BackHandler
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import com.barboza.bingomanager.controller.AppScreen
import com.barboza.bingomanager.controller.BingoController
import com.barboza.bingomanager.view.screens.CardEditorScreen
import com.barboza.bingomanager.view.screens.CustomModeEditorScreen
import com.barboza.bingomanager.view.screens.GameScreen
import com.barboza.bingomanager.view.screens.HomeScreen
import com.barboza.bingomanager.view.screens.ManageScreen
import com.barboza.bingomanager.view.screens.PhotoImportScreen
import com.barboza.bingomanager.view.screens.TemplateDesignerScreen

@Composable
fun BingoManagerApp(controller: BingoController) {
    val screen = controller.currentScreen
    BackHandler(enabled = screen != AppScreen.Home) {
        if (screen == AppScreen.Game) controller.finishGame() else controller.navigate(AppScreen.Home)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        when (screen) {
            AppScreen.Home -> HomeScreen(controller)
            is AppScreen.CardEditor -> CardEditorScreen(controller, screen)
            AppScreen.TemplateDesigner -> TemplateDesignerScreen(controller)
            AppScreen.Manage -> ManageScreen(controller)
            AppScreen.CustomModeEditor -> CustomModeEditorScreen(controller)
            AppScreen.PhotoImport -> PhotoImportScreen(controller)
            AppScreen.Game -> GameScreen(controller)
        }
    }
}
