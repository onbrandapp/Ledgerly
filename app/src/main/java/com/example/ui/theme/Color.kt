package com.example.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Minimalist Light Theme Palette (Matching Web Companion Overhaul)
val LightCanvasBackground = Color(0xFFF8FAFC)
val LightCardSurface = Color(0xFFFFFFFF)
val LightSurfaceAlt = Color(0xFFF1F5F9)
val LightBorder = Color(0xFFE2E8F0)
val LightBorderSubtle = Color(0xFFEDF2F7)

val SlateTextPrimary = Color(0xFF0F172A)
val SlateTextSecondary = Color(0xFF475569)
val SlateTextMuted = Color(0xFF64748B)

// Curated App Accent Colors for single-accent theme selection
data class AccentPreset(
    val id: String,
    val name: String,
    val hex: String,
    val color: Color
)

val AppAccentPresets = listOf(
    AccentPreset("espresso", "Espresso", "#392720", Color(0xFF392720)),
    AccentPreset("indigo", "Indigo", "#4F46E5", Color(0xFF4F46E5)),
    AccentPreset("emerald", "Emerald", "#059669", Color(0xFF059669)),
    AccentPreset("royal", "Royal Blue", "#2563EB", Color(0xFF2563EB)),
    AccentPreset("violet", "Violet", "#7C3AED", Color(0xFF7C3AED)),
    AccentPreset("amber", "Amber", "#D97706", Color(0xFFD97706)),
    AccentPreset("rose", "Rose", "#E11D48", Color(0xFFE11D48)),
    AccentPreset("slate", "Slate", "#0F172A", Color(0xFF0F172A))
)

// Bento Grid Theme Colors (Legacy references)
val BentoPrimary = Color(0xFF392720)
val BentoOnPrimary = Color(0xFFFFFFFF)
val BentoPrimaryContainer = Color(0xFFEEF2FF)
val BentoOnPrimaryContainer = Color(0xFF1E1B4B)

val BentoSecondary = Color(0xFF059669)
val BentoOnSecondary = Color(0xFFFFFFFF)
val BentoSecondaryContainer = Color(0xFFECFDF5)
val BentoOnSecondaryContainer = Color(0xFF064E3B)

val BentoBackground = Color(0xFFF8FAFC)
val BentoOnBackground = Color(0xFF0F172A)
val BentoSurface = Color(0xFFFFFFFF)
val BentoOnSurface = Color(0xFF0F172A)
val BentoSurfaceVariant = Color(0xFFF1F5F9)
val BentoOnSurfaceVariant = Color(0xFF475569)
val BentoOutline = Color(0xFFE2E8F0)

