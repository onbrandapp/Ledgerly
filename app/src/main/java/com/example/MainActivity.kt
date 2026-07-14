package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ExpenseViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val viewModel: ExpenseViewModel = viewModel()
                    val isLoggedIn by viewModel.isUserLoggedIn.collectAsState()

                    if (isLoggedIn) {
                        DashboardScreen(viewModel = viewModel)
                    } else {
                        LoginScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
