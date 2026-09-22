package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val DarkColorScheme = darkColorScheme(
    primary = BluePrimary,
    onPrimary = Color.White,
    primaryContainer = NavyCard,
    onPrimaryContainer = BlueLight,
    secondary = BlueLight,
    onSecondary = NavyDark,
    tertiary = GoldAccent,
    onTertiary = NavyDark,
    background = NavyDark,
    onBackground = Color.White,
    surface = NavySurface,
    onSurface = Color.White,
    surfaceVariant = NavyCard,
    onSurfaceVariant = BlueLight,
    outline = NavyBorder,
    error = RedDanger,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = Typography,
            content = content
        )
    }
}

