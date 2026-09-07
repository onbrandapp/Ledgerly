package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.BiometricLockScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ExpenseViewModel

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ExpenseViewModel = viewModel()
            val isLoggedIn by viewModel.isUserLoggedIn.collectAsState()
            val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
            val isBiometricUnlocked by viewModel.isBiometricUnlocked.collectAsState()
            val primaryHex by viewModel.primaryColor.collectAsState()
            val secondaryHex by viewModel.secondaryColor.collectAsState()
            val accentHex by viewModel.accentColor.collectAsState()

            MyApplicationTheme(
                primaryHex = primaryHex,
                secondaryHex = secondaryHex,
                accentHex = accentHex
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isLoggedIn) {
                        if (isBiometricEnabled && !isBiometricUnlocked) {
                            BiometricLockScreen(viewModel = viewModel)
                        } else {
                            DashboardScreen(viewModel = viewModel)
                        }
                    } else {
                        LoginScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
