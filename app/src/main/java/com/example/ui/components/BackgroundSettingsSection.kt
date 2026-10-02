package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppBackgroundPresets
import com.example.ui.theme.BackgroundType

@Composable
fun BackgroundSettingsSection(
    currentStyle: String,
    customHex: String,
    isDarkMode: Boolean,
    onSelectStyle: (String) -> Unit,
    onOpenCustomColorPicker: () -> Unit,
    onResetDefault: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("background_settings_section")
    ) {
        // Section Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = "App Background",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = "Select an ambient background atmosphere, textured dot grid, or custom background hue.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Scrollable Row of Preset Cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppBackgroundPresets.list.forEach { preset ->
                val isSelected = currentStyle.equals(preset.id, ignoreCase = true)
                val previewColors = if (isDarkMode) preset.darkColors else preset.lightColors

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        }
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .width(130.dp)
                        .clickable { onSelectStyle(preset.id) }
                        .testTag("background_preset_${preset.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Visual Preview Swatch
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                .then(
                                    when (preset.type) {
                                        BackgroundType.LINEAR_GRADIENT -> Modifier.background(
                                            Brush.verticalGradient(previewColors)
                                        )
                                        BackgroundType.PATTERN -> Modifier.background(
                                            if (isDarkMode) Color(0xFF0B0F19) else Color(0xFFF8FAFC)
                                        )
                                        else -> Modifier.background(
                                            if (isDarkMode) Color(0xFF0B0F19) else Color(0xFFF8FAFC)
                                        )
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            } else {
                                Icon(
                                    imageVector = preset.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Preset Title & Subtitle
                        Column {
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = preset.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Custom Color Canvas Option Card
        val isCustomSelected = currentStyle.equals("custom", ignoreCase = true)
        val customColor = remember(customHex) {
            try {
                Color(android.graphics.Color.parseColor(customHex))
            } catch (e: Exception) {
                Color(0xFFF8FAFC)
            }
        }

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isCustomSelected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                }
            ),
            border = BorderStroke(
                width = if (isCustomSelected) 2.dp else 1.dp,
                color = if (isCustomSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenCustomColorPicker() }
                .testTag("custom_background_color_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(customColor)
                            .border(
                                width = 2.dp,
                                color = if (isCustomSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCustomSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active Custom Color",
                                tint = if (customColor.red + customColor.green + customColor.blue > 1.8f) Color.Black else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.ColorLens,
                                contentDescription = null,
                                tint = if (customColor.red + customColor.green + customColor.blue > 1.8f) Color.Black else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Custom Solid Background",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isCustomSelected) "Active: $customHex (Tap to change)" else "Pick any custom color tint or hex",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = "Choose Hue",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // Reset to Default button if a customized background is active
        if (!currentStyle.equals("default", ignoreCase = true)) {
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedButton(
                onClick = onResetDefault,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier
                    .align(Alignment.End)
                    .height(32.dp)
                    .testTag("reset_background_button")
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Reset to Default Canvas",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp
                )
            }
        }
    }
}
