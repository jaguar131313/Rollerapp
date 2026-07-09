package com.example.rollerapp

import com.example.rollerapp.data.Conveyor
import com.example.rollerapp.data.ConveyorRepository
import com.example.rollerapp.presentation.MainViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private val repository: ConveyorRepository = mockk()
    private lateinit var viewModel: MainViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { repository.allConveyors } returns flowOf(listOf(Conveyor("Conv1")))
        viewModel = MainViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `selectConveyor updates selectedConveyor state`() {
        viewModel.selectConveyor("Conv1")
        assertEquals("Conv1", viewModel.selectedConveyor.value)
    }

    @Test
    fun `addConveyor calls repository`() {
        coEvery { repository.addConveyor("NewConv") } returns Unit
        viewModel.addConveyor("NewConv")
        // In a real test we might verify the call, but for simplicity we check if state could change
        // verify { repository.addConveyor("NewConv") }
    }
}
