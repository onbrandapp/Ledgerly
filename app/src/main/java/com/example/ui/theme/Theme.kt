package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun MyApplicationTheme(
  primaryHex: String = "#FFD97D",
  secondaryHex: String = "#A78BFA",
  accentHex: String = "#D9F99D",
  content: @Composable () -> Unit,
) {
  // Parse colors dynamically with graceful fallback
  val customPrimary = remember(primaryHex) {
    try { Color(android.graphics.Color.parseColor(primaryHex)) } catch (e: Exception) { Color(0xFFFFD97D) }
  }
  val customSecondary = remember(secondaryHex) {
    try { Color(android.graphics.Color.parseColor(secondaryHex)) } catch (e: Exception) { Color(0xFFA78BFA) }
  }
  val customAccent = remember(accentHex) {
    try { Color(android.graphics.Color.parseColor(accentHex)) } catch (e: Exception) { Color(0xFFD9F99D) }
  }

  // Premium Dark Charcoal/Slate scheme as requested
  val colorScheme = darkColorScheme(
    primary = customPrimary,
    onPrimary = Color(0xFF121214),       // high contrast dark text on light primary
    primaryContainer = customPrimary,
    onPrimaryContainer = Color(0xFF121214),

    secondary = customSecondary,
    onSecondary = Color(0xFF121214),
    secondaryContainer = customSecondary,
    onSecondaryContainer = Color(0xFF121214),

    tertiary = customAccent,
    onTertiary = Color(0xFF121214),
    tertiaryContainer = customAccent,
    onTertiaryContainer = Color(0xFF121214),

    // Polysure style Slate Charcoal Palette
    background = Color(0xFF0C0C0E),      // Extremely dark charcoal
    onBackground = Color(0xFFF1F1F5),    // Bright warm white
    
    surface = Color(0xFF16161A),         // Solid slate card background
    onSurface = Color(0xFFEDEDED),       // Very light gray
    
    surfaceVariant = Color(0xFF222226),  // Slightly lighter gray for input container
    onSurfaceVariant = Color(0xFFC4C4C8),// Medium grey for subtitles/secondary text
    
    outline = Color(0xFF2A2A2F),         // Clean dark divider border
    error = Color(0xFFFF8A80),
    onError = Color(0xFF121214)
  )

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
