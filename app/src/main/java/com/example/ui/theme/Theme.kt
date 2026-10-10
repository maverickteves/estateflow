package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = LimeAccent,
    onPrimary = NaturalDark,
    primaryContainer = ForestGreenDark,
    onPrimaryContainer = Color.White,
    secondary = LimeAccent,
    onSecondary = NaturalDark,
    background = DarkSurface,
    onBackground = Color.White,
    surface = NaturalDark,
    onSurface = Color.White,
    surfaceVariant = NaturalDarkAlt,
    onSurfaceVariant = SandWarm,
    outline = Slate700,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = ForestGreen,
    onPrimary = Color.White,
    primaryContainer = SandMuted,
    onPrimaryContainer = NaturalDark,
    secondary = LimeAccent,
    onSecondary = NaturalDark,
    background = NaturalBg,
    onBackground = NaturalDark,
    surface = Color.White,
    onSurface = NaturalDark,
    surfaceVariant = SandMuted,
    onSurfaceVariant = TaupeDark,
    outline = Color(0xFFB8B6AF),
  )

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(colorScheme = LightColorScheme, typography = Typography, content = content)
}
