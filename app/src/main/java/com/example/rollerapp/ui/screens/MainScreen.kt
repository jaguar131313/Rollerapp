package com.example.rollerapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.rollerapp.presentation.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val conveyorNames by viewModel.conveyors.collectAsState()
    val selectedConveyor by viewModel.selectedConveyor.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showLoginDialog by remember { mutableStateOf(false) }
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("🔧 Осмотр конвейеров") },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.syncData { result ->
                                scope.launch {
                                    val message = if (result.isSuccess) "Синхронизация завершена" else "Ошибка: ${result.exceptionOrNull()?.message}"
                                    snackbarHostState.showSnackbar(message)
                                }
                            }
                        },
                        enabled = !isSyncing
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync")
                        }
                    }
                    IconButton(onClick = onThemeToggle) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(
                                id = if (isDarkTheme) com.example.rollerapp.R.drawable.ic_sun else com.example.rollerapp.R.drawable.ic_moon
                            ),
                            contentDescription = "Toggle Theme"
                        )
                    }
                    // Admin button
                    IconButton(onClick = { /* handled below via state */ }) {
                        Icon(Icons.Default.Menu, contentDescription = "Admin")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (isSyncing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            
            ConveyorSelector(
                conveyors = conveyorNames.map { it.name },
                selected = selectedConveyor,
                onSelect = { viewModel.selectConveyor(it) },
                onAdd = { name, number -> viewModel.addConveyor(name, number) },
                onDelete = { viewModel.deleteConveyor(it) }
            )

            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }, text = { Text("Осмотры") })
                Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }, text = { Text("Замены") })
                Tab(selected = selectedTabIndex == 2, onClick = { selectedTabIndex = 2 }, text = { Text("Журнал") })
            }

            when (selectedTabIndex) {
                0 -> InspectionScreen(viewModel)
                1 -> ReplacementScreen(viewModel)
                2 -> JournalScreen(viewModel)
            }
        }
    }

}

@Composable
fun ConveyorSelector(
    conveyors: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    onAdd: (String, String) -> Unit,
    onDelete: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newNumber by remember { mutableStateOf("") }

    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Box(modifier = Modifier.weight(1f)) {
            var expanded by remember { mutableStateOf(false) }
            Button(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selected ?: "Выберите конвейер")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                conveyors.forEach { name ->
                    DropdownMenuItem(text = { Text(name) }, onClick = {
                        onSelect(name)
                        expanded = false
                    })
                }
            }
        }
        
        IconButton(onClick = { showAddDialog = true }) { Icon(Icons.Default.Add, "Add") }
        IconButton(onClick = { selected?.let { onDelete(it) } }) { Icon(Icons.Default.Delete, "Delete") }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Добавить конвейер") },
            text = {
                Column {
                    TextField(value = newName, onValueChange = { newName = it }, label = { Text("Имя конвейера") })
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(value = newNumber, onValueChange = { newNumber = it }, label = { Text("Номер конвейера") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newName.isNotBlank()) {
                        onAdd(newName, newNumber)
                        newName = ""
                        newNumber = ""
                        showAddDialog = false
                    }
                }) { Text("Добавить") }
            }
        )
    }
}

// Screen implementations are in separate files
