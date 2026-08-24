package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PolishColorScheme = lightColorScheme(
    primary = PolishPrimary,
    onPrimary = PolishOnPrimary,
    primaryContainer = PolishPrimaryContainer,
    onPrimaryContainer = PolishPrimaryDark,
    secondary = PolishSecondary,
    onSecondary = Color.White,
    secondaryContainer = PolishSecondaryContainer,
    onSecondaryContainer = PolishPrimaryDark,
    tertiary = PolishPrimaryLight,
    onTertiary = PolishPrimaryDark,
    background = PolishBg,
    onBackground = TextPrimary,
    surface = PolishSurface,
    onSurface = TextPrimary,
    surfaceVariant = PolishSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = PolishCardBorder,
    outlineVariant = PolishCardBorderLight,
    error = AccentRed,
    onError = Color.White,
    errorContainer = AccentRedContainer,
    onErrorContainer = AccentRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PolishColorScheme,
        typography = Typography,
        content = content
    )
}

