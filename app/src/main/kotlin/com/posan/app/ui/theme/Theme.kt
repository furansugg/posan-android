package com.posan.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Brand500,
    onPrimary = Color.White,
    primaryContainer = Brand100,
    onPrimaryContainer = Brand800,
    secondary = Slate600,
    onSecondary = Color.White,
    secondaryContainer = Slate100,
    onSecondaryContainer = Slate800,
    tertiary = Accent500,
    onTertiary = Color.White,
    tertiaryContainer = Accent100,
    onTertiaryContainer = Color(0xFF064E3B),
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    surfaceTint = Brand500,
    outline = Slate300,
    outlineVariant = Slate200,
    error = Danger500,
    onError = Color.White,
    errorContainer = Danger100,
    onErrorContainer = Color(0xFF7F1D1D)
)

private val DarkColors = darkColorScheme(
    primary = Brand400,
    onPrimary = Slate900,
    primaryContainer = Brand700,
    onPrimaryContainer = Brand100,
    secondary = Slate300,
    secondaryContainer = Slate700,
    onSecondaryContainer = Slate100,
    tertiary = Accent400,
    onTertiary = Slate900,
    tertiaryContainer = Color(0xFF064E3B),
    onTertiaryContainer = Accent100,
    background = Slate900,
    onBackground = Slate50,
    surface = Slate800,
    onSurface = Slate50,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate300,
    outline = Slate600,
    outlineVariant = Slate700,
    error = Danger500,
    errorContainer = Color(0xFF450A0A),
    onErrorContainer = Danger100
)

@Composable
fun PosanTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (useDarkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = PosanTypography,
        shapes = PosanShapes,
        content = content
    )
}
