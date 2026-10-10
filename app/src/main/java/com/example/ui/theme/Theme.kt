package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

val LocalAppAccent = compositionLocalOf { Color(0xFF392720) }

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false,
  primaryHex: String = "#392720",
  secondaryHex: String = "#392720",
  accentHex: String = "#392720",
  content: @Composable () -> Unit,
) {
  // Resolve the 1 unified accent color from incoming parameters
  // Prioritizes accentHex or primaryHex if customized from legacy defaults
  val effectiveHex = when {
    accentHex.isNotBlank() && accentHex != "#D9F99D" -> accentHex
    primaryHex.isNotBlank() && primaryHex != "#FFD97D" -> primaryHex
    secondaryHex.isNotBlank() && secondaryHex != "#A78BFA" -> secondaryHex
    else -> "#392720"
  }

  // Parse accent color with graceful fallback to signature accent (#392720)
  val customAccent = remember(effectiveHex) {
    try {
      Color(android.graphics.Color.parseColor(effectiveHex))
    } catch (e: Exception) {
      Color(0xFF392720)
    }
  }

  // Calculate contrast luminance on the selected accent
  val isLightAccent = remember(customAccent) {
    val luminance = (0.299 * customAccent.red + 0.587 * customAccent.green + 0.114 * customAccent.blue)
    luminance > 0.65
  }
  val onAccentColor = if (isLightAccent) Color(0xFF0F172A) else Color.White

  // Minimalist Light Theme matching the web overhaul (kept exactly as-is)
  val lightColors = lightColorScheme(
    primary = customAccent,
    onPrimary = onAccentColor,
    primaryContainer = customAccent.copy(alpha = 0.12f),
    onPrimaryContainer = if (isLightAccent) Color(0xFF0F172A) else customAccent,

    secondary = customAccent,
    onSecondary = onAccentColor,
    secondaryContainer = customAccent.copy(alpha = 0.08f),
    onSecondaryContainer = if (isLightAccent) Color(0xFF0F172A) else customAccent,

    tertiary = customAccent,
    onTertiary = onAccentColor,
    tertiaryContainer = customAccent.copy(alpha = 0.15f),
    onTertiaryContainer = if (isLightAccent) Color(0xFF0F172A) else customAccent,

    // Minimalist Clean Canvas Palette
    background = Color(0xFFF8FAFC),       // Clean soft off-white background (slate-50)
    onBackground = Color(0xFF0F172A),     // Crisp deep slate text (slate-900)

    surface = Color(0xFFFFFFFF),          // Pure white card surfaces
    onSurface = Color(0xFF0F172A),        // Deep slate text on surfaces

    surfaceVariant = Color(0xFFF1F5F9),   // Light slate input / chip container (slate-100)
    onSurfaceVariant = Color(0xFF475569), // Muted slate for subtitles/secondary text (slate-600)

    outline = Color(0xFFE2E8F0),          // Clean light divider border (slate-200)
    outlineVariant = Color(0xFFEDF2F7),   // Subtle secondary divider
    surfaceTint = Color.Transparent,
    
    error = Color(0xFFEF4444),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B)
  )

  // Polished Dark Theme using Matte Black as the base palette
  val darkColors = darkColorScheme(
    primary = customAccent,
    onPrimary = onAccentColor,
    primaryContainer = customAccent.copy(alpha = 0.25f),
    onPrimaryContainer = Color(0xFFEDEDED),

    secondary = customAccent,
    onSecondary = onAccentColor,
    secondaryContainer = customAccent.copy(alpha = 0.20f),
    onSecondaryContainer = Color(0xFFEDEDED),

    tertiary = customAccent,
    onTertiary = onAccentColor,
    tertiaryContainer = customAccent.copy(alpha = 0.28f),
    onTertiaryContainer = Color(0xFFEDEDED),

    // Matte Black Canvas & Surfaces (Zero blue tint, clean luxury dark)
    background = Color(0xFF0F0F0F),       // Deep matte black canvas
    onBackground = Color(0xFFEDEDED),     // Crisp bright off-white text

    surface = Color(0xFF181818),          // Distinct elevated matte black card surfaces
    onSurface = Color(0xFFEDEDED),        // Crisp bright off-white text on surfaces

    surfaceVariant = Color(0xFF242424),   // Matte dark graphite container / chip / input surface
    onSurfaceVariant = Color(0xFFA3A3A3), // Clean neutral muted gray for subtitles & secondary labels

    outline = Color(0xFF383838),          // Neutral dark border / dividers
    outlineVariant = Color(0xFF262626),   // Subtle dark separator
    surfaceTint = Color.Transparent,

    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFECACA)
  )

  val colorScheme = if (darkTheme) darkColors else lightColors

  CompositionLocalProvider(LocalAppAccent provides customAccent) {
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
  }
}


