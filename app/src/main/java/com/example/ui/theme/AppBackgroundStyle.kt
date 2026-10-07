package com.example.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class BackgroundType {
    SOLID,
    LINEAR_GRADIENT,
    PATTERN
}

data class BackgroundPreset(
    val id: String,
    val name: String,
    val subtitle: String,
    val lightColors: List<Color>,
    val darkColors: List<Color>,
    val icon: ImageVector,
    val type: BackgroundType = BackgroundType.LINEAR_GRADIENT
)

object AppBackgroundPresets {
    val DEFAULT = BackgroundPreset(
        id = "default",
        name = "Default Matte",
        subtitle = "Clean minimalist canvas",
        lightColors = listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9)),
        darkColors = listOf(Color(0xFF0F0F0F), Color(0xFF181818)),
        icon = Icons.Default.Landscape,
        type = BackgroundType.SOLID
    )

    val AURORA = BackgroundPreset(
        id = "aurora",
        name = "Aurora Glow",
        subtitle = "Warm peach & lavender aura",
        lightColors = listOf(Color(0xFFFFF1F2), Color(0xFFF5F3FF), Color(0xFFEFF6FF)),
        darkColors = listOf(Color(0xFF1F112E), Color(0xFF12142B), Color(0xFF19122C)),
        icon = Icons.Default.AutoAwesome,
        type = BackgroundType.LINEAR_GRADIENT
    )

    val OCEAN = BackgroundPreset(
        id = "ocean",
        name = "Ocean Mist",
        subtitle = "Cool aqua & azure breeze",
        lightColors = listOf(Color(0xFFF0FDFA), Color(0xFFECFEFF), Color(0xFFE0F2FE)),
        darkColors = listOf(Color(0xFF042F2E), Color(0xFF06334D), Color(0xFF0A1E33)),
        icon = Icons.Default.WaterDrop,
        type = BackgroundType.LINEAR_GRADIENT
    )

    val WARM_LINEN = BackgroundPreset(
        id = "warm_linen",
        name = "Warm Linen",
        subtitle = "Cozy parchment & latte tones",
        lightColors = listOf(Color(0xFFFDFBF7), Color(0xFFF7F2E7), Color(0xFFEFE6D5)),
        darkColors = listOf(Color(0xFF1E1915), Color(0xFF191410), Color(0xFF261F1A)),
        icon = Icons.Default.Spa,
        type = BackgroundType.LINEAR_GRADIENT
    )

    val EMERALD = BackgroundPreset(
        id = "emerald",
        name = "Emerald Grove",
        subtitle = "Botanical sage & serene mint",
        lightColors = listOf(Color(0xFFF0FDF4), Color(0xFFECFDF5), Color(0xFFDCFCE7)),
        darkColors = listOf(Color(0xFF052E16), Color(0xFF064E3B), Color(0xFF08281B)),
        icon = Icons.Default.Spa,
        type = BackgroundType.LINEAR_GRADIENT
    )

    val MIDNIGHT = BackgroundPreset(
        id = "midnight",
        name = "Midnight Nebula",
        subtitle = "Deep cosmic indigo & starlight",
        lightColors = listOf(Color(0xFFEEF2FF), Color(0xFFF5F3FF), Color(0xFFE0E7FF)),
        darkColors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF070B14)),
        icon = Icons.Default.DarkMode,
        type = BackgroundType.LINEAR_GRADIENT
    )

    val SUNSET = BackgroundPreset(
        id = "sunset",
        name = "Golden Sunset",
        subtitle = "Warm amber & coral radiance",
        lightColors = listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7), Color(0xFFFFEDD5)),
        darkColors = listOf(Color(0xFF2E1906), Color(0xFF201306), Color(0xFF160D05)),
        icon = Icons.Default.WbSunny,
        type = BackgroundType.LINEAR_GRADIENT
    )

    val DOT_GRID = BackgroundPreset(
        id = "dot_grid",
        name = "Dot Matrix",
        subtitle = "Modern geometric dot grid",
        lightColors = listOf(Color(0xFFF8FAFC)),
        darkColors = listOf(Color(0xFF0F0F0F)),
        icon = Icons.Default.GridOn,
        type = BackgroundType.PATTERN
    )

    val list = listOf(
        DEFAULT,
        AURORA,
        OCEAN,
        WARM_LINEN,
        EMERALD,
        MIDNIGHT,
        SUNSET,
        DOT_GRID
    )

    fun getById(id: String): BackgroundPreset {
        return list.find { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
    }
}

/**
 * High-performance App Background Container that wraps top-level screens
 * and gracefully renders solid, gradient, dot grid, or custom tinted backgrounds.
 */
@Composable
fun AppBackgroundContainer(
    backgroundStyle: String,
    customBackgroundHex: String,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isCustom = backgroundStyle.equals("custom", ignoreCase = true)
    val preset = remember(backgroundStyle) { AppBackgroundPresets.getById(backgroundStyle) }

    val customColor = remember(customBackgroundHex) {
        try {
            Color(android.graphics.Color.parseColor(customBackgroundHex))
        } catch (e: Exception) {
            if (isDarkMode) Color(0xFF0F0F0F) else Color(0xFFF8FAFC)
        }
    }

    val activeColors = remember(preset, isDarkMode) {
        if (isDarkMode) preset.darkColors else preset.lightColors
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            isCustom -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(customColor)
                )
            }
            preset.type == BackgroundType.PATTERN -> {
                val baseColor = if (isDarkMode) Color(0xFF0F0F0F) else Color(0xFFF8FAFC)
                val dotColor = if (isDarkMode) Color(0xFFA3A3A3).copy(alpha = 0.18f) else Color(0xFF475569).copy(alpha = 0.12f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(baseColor)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val spacingPx = 28.dp.toPx()
                        val dotRadiusPx = 1.3.dp.toPx()
                        var x = spacingPx / 2
                        while (x < size.width) {
                            var y = spacingPx / 2
                            while (y < size.height) {
                                drawCircle(
                                    color = dotColor,
                                    radius = dotRadiusPx,
                                    center = Offset(x, y)
                                )
                                y += spacingPx
                            }
                            x += spacingPx
                        }
                    }
                }
            }
            preset.type == BackgroundType.LINEAR_GRADIENT -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = activeColors,
                                startY = 0f,
                                endY = Float.POSITIVE_INFINITY
                            )
                        )
                )
            }
            else -> {
                // Default Solid
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isDarkMode) Color(0xFF0F0F0F) else Color(0xFFF8FAFC))
                )
            }
        }

        // Render screen content on top
        content()
    }
}
