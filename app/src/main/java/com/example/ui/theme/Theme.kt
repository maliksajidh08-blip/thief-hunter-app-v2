package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = YellowAccent,
    onPrimary = NavyDark,
    primaryContainer = NavyLight,
    onPrimaryContainer = YellowAccent,
    secondary = YellowContainer,
    onSecondary = NavyDark,
    tertiary = InfoCyan,
    background = BackgroundDark,
    surface = NavySurfaceDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = NavyCardDark,
    onSurfaceVariant = TextSecondaryDark,
    error = AlertRed,
    onError = Color.White
  )

private val LightColorScheme =
  lightColorScheme(
    primary = NavyPrimary,
    onPrimary = Color.White,
    primaryContainer = NavyLight,
    onPrimaryContainer = Color.White,
    secondary = YellowDark,
    onSecondary = NavyDark,
    secondaryContainer = YellowAccent,
    onSecondaryContainer = NavyDark,
    tertiary = InfoCyan,
    background = BackgroundLight,
    surface = SurfaceLight,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFE8EAF6),
    onSurfaceVariant = TextSecondaryLight,
    error = AlertRed,
    onError = Color.White
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Use intentional Thief Hunter Navy & Yellow branding
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

