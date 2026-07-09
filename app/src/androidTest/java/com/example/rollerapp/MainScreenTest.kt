package com.example.rollerapp

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.rollerapp.data.Conveyor
import com.example.rollerapp.data.ConveyorRepository
import com.example.rollerapp.presentation.MainViewModel
import com.example.rollerapp.ui.screens.MainScreen
import com.example.rollerapp.ui.theme.RollerAppTheme
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MainScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val repository: ConveyorRepository = mockk()

    @Before
    fun setUp() {
        every { repository.allConveyors } returns flowOf(listOf(Conveyor("Test Conveyor")))
        // Add more mocks as needed for the ViewModel init
    }

    @Test
    fun mainScreen_showsTitle() {
        val viewModel = MainViewModel(repository)
        
        composeTestRule.setContent {
            RollerAppTheme {
                MainScreen(
                    viewModel = viewModel,
                    isDarkTheme = false,
                    onThemeToggle = {}
                )
            }
        }

        composeTestRule.onNodeWithText("🔧 Осмотр конвейеров").assertExists()
    }
}
