package com.barboza.bingomanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.barboza.bingomanager.controller.BingoController
import com.barboza.bingomanager.data.BingoRepository
import com.barboza.bingomanager.ui.theme.BingoManagerTheme
import com.barboza.bingomanager.view.BingoManagerApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val controller = BingoController(BingoRepository(applicationContext))
        setContent {
            BingoManagerTheme {
                BingoManagerApp(controller)
            }
        }
    }
}
