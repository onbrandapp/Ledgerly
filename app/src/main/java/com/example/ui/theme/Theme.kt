package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = BentoPrimary,
    secondary = BentoSecondary,
    tertiary = BentoPrimaryContainer,
    background = Color(0xFF11140E),
    surface = Color(0xFF191C17),
    onPrimary = BentoOnPrimary,
    onSecondary = BentoOnSecondary,
    onBackground = Color(0xFFE2E3DC),
    onSurface = Color(0xFFE2E3DC),
    primaryContainer = BentoPrimaryContainer,
    secondaryContainer = BentoSecondaryContainer,
    surfaceVariant = Color(0xFF43483E),
    onSurfaceVariant = Color(0xFFC3C8BB),
    outline = BentoOutline
  )

private val LightColorScheme =
  lightColorScheme(
    primary = BentoPrimary,
    onPrimary = BentoOnPrimary,
    primaryContainer = BentoPrimaryContainer,
    onPrimaryContainer = BentoOnPrimaryContainer,
    secondary = BentoSecondary,
    onSecondary = BentoOnSecondary,
    secondaryContainer = BentoSecondaryContainer,
    onSecondaryContainer = BentoOnSecondaryContainer,
    background = BentoBackground,
    onBackground = BentoOnBackground,
    surface = BentoSurface,
    onSurface = BentoOnSurface,
    surfaceVariant = BentoSurfaceVariant,
    onSurfaceVariant = BentoOnSurfaceVariant,
    outline = BentoOutline
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is disabled by default to preserve the custom Bento Grid theme
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
