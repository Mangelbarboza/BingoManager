package com.barboza.bingomanager.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BingoRed,
    secondary = MediumGray,
    background = Color(0xFF171717),
    surface = Color(0xFF222222),
    onPrimary = Color.White,
    onBackground = Color(0xFFF4F4F4),
    onSurface = Color(0xFFF4F4F4),
)

private val LightColorScheme = lightColorScheme(
    primary = BingoRed,
    secondary = MediumGray,
    tertiary = BingoRedDark,
    background = WarmWhite,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Charcoal,
    onSurface = Charcoal,
    surfaceVariant = LightGray,
    outline = BorderGray,
)

@Composable
fun BingoManagerTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
