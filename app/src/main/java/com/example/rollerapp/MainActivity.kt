package com.example.rollerapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rollerapp.data.AppDatabase
import com.example.rollerapp.data.ConveyorRepository
import com.example.rollerapp.presentation.MainViewModel
import com.example.rollerapp.presentation.ViewModelFactory
import com.example.rollerapp.ui.theme.RollerAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = AppDatabase.getInstance(this)
        val repository = ConveyorRepository(database.rollerDao())
        val factory = ViewModelFactory(repository)

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)

        setContent {
            var isDarkTheme by remember { mutableStateOf(prefs.getBoolean("dark_theme", false)) }
            val mainViewModel: MainViewModel = viewModel(factory = factory)
            val isSignedIn by mainViewModel.isSignedIn.collectAsState()

            RollerAppTheme(darkTheme = isDarkTheme) {
                Surface(
                                        modifier = Modifier.fillMaxSize(),
                                        color = MaterialTheme.colorScheme.background
                ) {
                                        if (!isSignedIn) {
                                            com.example.rollerapp.ui.screens.LoginScreen(mainViewModel)
                                        } else {
                                            com.example.rollerapp.ui.screens.MainScreen(
                                                viewModel = mainViewModel,
                                                isDarkTheme = isDarkTheme,
                                                onThemeToggle = {
                                                    isDarkTheme = !isDarkTheme
                                                    prefs.edit().putBoolean("dark_theme", isDarkTheme).apply()
                                                }
                                            )
                                        }
                }
            }
        }
    }
}
