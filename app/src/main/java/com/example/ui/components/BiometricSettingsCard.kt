package com.example.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.BiometricAuthManager
import com.example.security.BiometricStatus
import com.example.ui.screens.findFragmentActivity
import com.example.ui.viewmodel.ExpenseViewModel

@Composable
fun BiometricSettingsCard(
    viewModel: ExpenseViewModel,
    onDismissParent: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val biometricStatus = remember(context) { BiometricAuthManager.checkBiometricStatus(context) }
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()

    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isSuccessFeedback by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("biometric_settings_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                color = if (isBiometricEnabled) 
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) 
                                else 
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = if (isBiometricEnabled) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Biometric App Lock",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when (biometricStatus) {
                                BiometricStatus.READY -> "Require fingerprint / face unlock to view financial data"
                                BiometricStatus.NOT_ENROLLED -> "No biometric enrolled in device settings"
                                BiometricStatus.UNAVAILABLE -> "Biometric hardware unavailable on this device"
                                BiometricStatus.UNKNOWN -> "Fingerprint or face unlock security"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }

                Switch(
                    checked = isBiometricEnabled,
                    onCheckedChange = { targetState ->
                        feedbackMessage = null
                        if (targetState) {
                            if (activity != null && biometricStatus == BiometricStatus.READY) {
                                BiometricAuthManager.showBiometricPrompt(
                                    activity = activity,
                                    title = "Enable Biometric Security",
                                    subtitle = "Confirm fingerprint or face unlock to secure Ledgerly",
                                    negativeButtonText = "Cancel",
                                    onSuccess = {
                                        viewModel.setBiometricEnabled(true)
                                        isSuccessFeedback = true
                                        feedbackMessage = "Biometric app lock enabled successfully!"
                                    },
                                    onError = { error ->
                                        isSuccessFeedback = false
                                        feedbackMessage = error
                                    }
                                )
                            } else {
                                // Direct toggle if hardware isn't registered (e.g. emulator testing)
                                viewModel.setBiometricEnabled(true)
                                isSuccessFeedback = true
                                feedbackMessage = "Biometric protection active."
                            }
                        } else {
                            if (activity != null && biometricStatus == BiometricStatus.READY) {
                                BiometricAuthManager.showBiometricPrompt(
                                    activity = activity,
                                    title = "Disable Biometric Security",
                                    subtitle = "Confirm your identity to turn off biometric lock",
                                    negativeButtonText = "Cancel",
                                    onSuccess = {
                                        viewModel.setBiometricEnabled(false)
                                        isSuccessFeedback = true
                                        feedbackMessage = "Biometric lock disabled."
                                    },
                                    onError = { error ->
                                        isSuccessFeedback = false
                                        feedbackMessage = error
                                    }
                                )
                            } else {
                                viewModel.setBiometricEnabled(false)
                                isSuccessFeedback = true
                                feedbackMessage = "Biometric lock disabled."
                            }
                        }
                    },
                    modifier = Modifier.testTag("biometric_lock_switch")
                )
            }

            AnimatedVisibility(visible = feedbackMessage != null) {
                feedbackMessage?.let { msg ->
                    Surface(
                        color = if (isSuccessFeedback) 
                            Color(0xFF2E7D32).copy(alpha = 0.12f) 
                        else 
                            MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isSuccessFeedback) Icons.Default.CheckCircle else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (isSuccessFeedback) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSuccessFeedback) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            if (isBiometricEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (activity != null) {
                                BiometricAuthManager.showBiometricPrompt(
                                    activity = activity,
                                    title = "Test Biometrics",
                                    subtitle = "Testing fingerprint and face recognition sensors",
                                    negativeButtonText = "Cancel",
                                    onSuccess = {
                                        isSuccessFeedback = true
                                        feedbackMessage = "Biometric verification succeeded!"
                                    },
                                    onError = { error ->
                                        isSuccessFeedback = false
                                        feedbackMessage = error
                                    }
                                )
                            } else {
                                feedbackMessage = "Activity not found."
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("test_biometric_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Test Sensor", fontSize = 13.sp)
                        }
                    }

                    FilledTonalButton(
                        onClick = {
                            onDismissParent()
                            viewModel.lockApp()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("lock_app_now_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Lock App", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
