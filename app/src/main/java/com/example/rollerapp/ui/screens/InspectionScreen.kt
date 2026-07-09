package com.example.rollerapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.rollerapp.presentation.MainViewModel
import com.example.rollerapp.data.Inspection
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun InspectionScreen(viewModel: MainViewModel) {
    val inspections by viewModel.filteredInspections.collectAsState()
    val rawInspections by viewModel.inspections.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedDamages by viewModel.selectedDamages.collectAsState()
    val selectedRollers by viewModel.selectedRollers.collectAsState()
    
    var showDialog by remember { mutableStateOf(false) }
    var editingInspection by remember { mutableStateOf<Inspection?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            StatisticsCard(rawInspections)

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Поиск по опоре, ролику...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(Icons.Default.Menu, contentDescription = "Filters")
                    }
                }
            )

            AnimatedVisibility(visible = showFilters) {
                FilterPanel(
                    selectedDamages = selectedDamages,
                    selectedRollers = selectedRollers,
                    onDamageToggle = { viewModel.toggleDamageFilter(it) },
                    onRollerToggle = { viewModel.toggleRollerFilter(it) },
                    onReset = { viewModel.resetFilters() }
                )
            }

            Button(
                onClick = { 
                    editingInspection = null
                    showDialog = true 
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Добавить осмотр")
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                var showImportDialog by remember { mutableStateOf(false) }
                val context = androidx.compose.ui.platform.LocalContext.current
                
                OutlinedButton(
                    onClick = {
                        val text = inspections.joinToString("\n") { 
                            "${it.opora} ${it.roller} ${it.damage} ${if (it.spare) "Да" else "Нет"} ${it.nearestRoller ?: ""}" 
                        }
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Inspections", text)
                        clipboard.setPrimaryClip(clip)
                        scope.launch { snackbarHostState.showSnackbar("Скопировано в буфер обмена") }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("📋 Копировать текст")
                }
                OutlinedButton(
                    onClick = { showImportDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("📝 Вставить текст")
                }

                if (showImportDialog) {
                    var importText by remember { mutableStateOf("") }
                    AlertDialog(
                        onDismissRequest = { showImportDialog = false },
                        title = { Text("Импорт из текста") },
                        text = { OutlinedTextField(value = importText, onValueChange = { importText = it }, label = { Text("Вставьте данные") }) },
                        confirmButton = {
                            Button(onClick = { 
                                viewModel.importFromText(importText)
                                showImportDialog = false 
                                scope.launch { snackbarHostState.showSnackbar("Импорт завершен") }
                            }) { Text("Импортировать") }
                        }
                    )
                }
            }

            val context = androidx.compose.ui.platform.LocalContext.current
            Button(
                onClick = { 
                    viewModel.exportToExcel(
                        context, 
                        "inspections",
                        onComplete = { scope.launch { snackbarHostState.showSnackbar(it) } },
                        onError = { scope.launch { snackbarHostState.showSnackbar(it) } }
                    )
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.Menu, null)
                Spacer(Modifier.width(8.dp))
                Text("📊 Сохранить в Excel")
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(inspections) { item ->
                    InspectionCard(
                        item, 
                        onEdit = {
                            editingInspection = item
                            showDialog = true
                        },
                        onDelete = { viewModel.deleteInspection(item, it) }
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showDialog) {
        InspectionDialog(
            inspection = editingInspection,
            onDismiss = { showDialog = false },
            onConfirm = { opora, roller, damage, spare, nearest ->
                if (editingInspection == null) {
                    viewModel.addInspection(opora, roller, damage, spare, nearest)
                } else {
                    viewModel.updateInspection(editingInspection!!.copy(opora = opora, roller = roller, damage = damage, spare = spare, nearestRoller = nearest))
                }
                showDialog = false
            }
        )
    }
}

@Composable
fun StatisticsCard(inspections: List<Inspection>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Статистика", style = MaterialTheme.typography.titleSmall)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Всего: ${inspections.size}", style = MaterialTheme.typography.bodyLarge)
                    val topDamage = inspections.groupBy { it.damage }.maxByOrNull { it.value.size }?.key ?: "—"
                    Text("Топ повреждений: $topDamage", style = MaterialTheme.typography.bodySmall)
                }
                val spareCount = inspections.count { it.spare }
                Text("В запасе: $spareCount", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterPanel(
    selectedDamages: Set<String>,
    selectedRollers: Set<String>,
    onDamageToggle: (String) -> Unit,
    onRollerToggle: (String) -> Unit,
    onReset: () -> Unit
) {
    val rollers = listOf("Рабочий правый", "Рабочий левый", "Рабочий центральный", "Поддерживающий правый", "Поддерживающий левый")
    val damages = listOf("Клин", "Рассыпался подшипник", "Шум", "Сильный шум")

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text("Ролики:", style = MaterialTheme.typography.labelSmall)
        FlowRow(modifier = Modifier.fillMaxWidth()) {
            rollers.forEach { r ->
                FilterChip(
                    selected = selectedRollers.contains(r),
                    onClick = { onRollerToggle(r) },
                    label = { Text(r, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }
        Text("Повреждения:", style = MaterialTheme.typography.labelSmall)
        FlowRow(modifier = Modifier.fillMaxWidth()) {
            damages.forEach { d ->
                FilterChip(
                    selected = selectedDamages.contains(d),
                    onClick = { onDamageToggle(d) },
                    label = { Text(d, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }
        TextButton(onClick = onReset, modifier = Modifier.align(Alignment.End)) {
            Text("Сбросить фильтры")
        }
    }
}

@Composable
fun InspectionCard(inspection: Inspection, onEdit: () -> Unit, onDelete: (Boolean) -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(inspection.timestamp))

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Опора: ${inspection.opora}", style = MaterialTheme.typography.titleMedium)
                Text(date, style = MaterialTheme.typography.bodySmall)
            }
            Text("Ролик: ${inspection.roller}")
            Text("Повреждение: ${inspection.damage}")
            Text("Запасной: ${if (inspection.spare) "Да" else "Нет"}")
            inspection.nearestRoller?.let { Text("Ближайший: $it") }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit") }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить запись?") },
            text = { Text("Перенести запись в историю замен?") },
            confirmButton = {
                TextButton(onClick = { 
                    onDelete(true)
                    showDeleteDialog = false 
                }) { Text("Да (в историю)") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    onDelete(false)
                    showDeleteDialog = false 
                }) { Text("Нет (просто удалить)") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionDialog(
    inspection: Inspection?,
    onDismiss: () -> Unit,
    onConfirm: (Int, String, String, Boolean, String?) -> Unit
) {
    var opora by remember { mutableStateOf(inspection?.opora?.toString() ?: "") }
    var roller by remember { mutableStateOf(inspection?.roller ?: "Рабочий правый") }
    var damage by remember { mutableStateOf(inspection?.damage ?: "Клин") }
    var spare by remember { mutableStateOf(inspection?.spare ?: false) }
    var nearest by remember { mutableStateOf(inspection?.nearestRoller ?: "") }

    val rollers = listOf("Рабочий правый", "Рабочий левый", "Рабочий центральный", "Поддерживающий правый", "Поддерживающий левый")
    val damages = listOf("Клин", "Рассыпался подшипник", "Шум", "Сильный шум", "Другое")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (inspection == null) "Новый осмотр" else "Редактировать осмотр") },
        text = {
            Column {
                TextField(
                    value = opora, 
                    onValueChange = { opora = it }, 
                    label = { Text("Опора (1-1200)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                
                var rollerExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = rollerExpanded, onExpandedChange = { rollerExpanded = it }) {
                    TextField(value = roller, onValueChange = {}, readOnly = true, label = { Text("Ролик") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rollerExpanded) }, modifier = Modifier.menuAnchor())
                    ExposedDropdownMenu(expanded = rollerExpanded, onDismissRequest = { rollerExpanded = false }) {
                        rollers.forEach { r -> DropdownMenuItem(text = { Text(r) }, onClick = { roller = r; rollerExpanded = false }) }
                    }
                }

                var damageExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = damageExpanded, onExpandedChange = { damageExpanded = it }) {
                    TextField(value = damage, onValueChange = {}, readOnly = true, label = { Text("Повреждение") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = damageExpanded) }, modifier = Modifier.menuAnchor())
                    ExposedDropdownMenu(expanded = damageExpanded, onDismissRequest = { damageExpanded = false }) {
                        damages.forEach { d -> DropdownMenuItem(text = { Text(d) }, onClick = { damage = d; damageExpanded = false }) }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = spare, onCheckedChange = { spare = it })
                    Text("Запасной ролик")
                }
                TextField(value = nearest, onValueChange = { nearest = it }, label = { Text("Ближайший ролик") })
            }
        },
        confirmButton = {
            Button(onClick = {
                val oporaInt = opora.toIntOrNull() ?: 0
                onConfirm(oporaInt, roller, damage, spare, nearest.ifBlank { null })
            }) { Text(if (inspection == null) "ОК" else "Сохранить") }
        }
    )
}
