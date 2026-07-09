package com.example.rollerapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.rollerapp.presentation.MainViewModel
import com.example.rollerapp.data.Replacement
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReplacementScreen(viewModel: MainViewModel) {
    val replacements by viewModel.replacements.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingReplacement by remember { mutableStateOf<Replacement?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var sortBy by remember { mutableStateOf("timestamp") }
    var ascending by remember { mutableStateOf(false) }

    val sorted = remember(replacements, sortBy, ascending) {
        replacements.sortedWith(compareBy<Replacement> { r ->
            when (sortBy) {
                "opora" -> r.opora
                "roller" -> r.roller
                "reason" -> r.reason
                else -> r.timestamp.toInt()
            }
        }).let { if (ascending) it else it.reversed() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            Button(onClick = {
                editingReplacement = null
                showDialog = true
            }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, null)
                Text("Вручную добавить замену")
            }

            val context = androidx.compose.ui.platform.LocalContext.current
            Button(
                onClick = {
                    viewModel.exportToExcel(
                        context,
                        "replacements",
                        onComplete = { scope.launch { snackbarHostState.showSnackbar(it) } },
                        onError = { scope.launch { snackbarHostState.showSnackbar(it) } }
                    )
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.Menu, null)
                Spacer(Modifier.width(8.dp))
                Text("📊 Сохранить в Excel")
            }

            Spacer(Modifier.height(8.dp))

            // Table header
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                TableHeader("Опора", "opora", sortBy, ascending) { key ->
                    if (sortBy == key) ascending = !ascending else { sortBy = key; ascending = true }
                }
                TableHeader("Ролик", "roller", sortBy, ascending) { key ->
                    if (sortBy == key) ascending = !ascending else { sortBy = key; ascending = true }
                }
                TableHeader("Причина", "reason", sortBy, ascending) { key ->
                    if (sortBy == key) ascending = !ascending else { sortBy = key; ascending = true }
                }
                TableHeader("Дата", "timestamp", sortBy, ascending) { key ->
                    if (sortBy == key) ascending = !ascending else { sortBy = key; ascending = true }
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(sorted) { item ->
                    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(item.opora.toString(), modifier = Modifier.weight(1f))
                        Text(item.roller, modifier = Modifier.weight(3f))
                        Text(item.reason, modifier = Modifier.weight(3f))
                        Text(java.text.SimpleDateFormat("dd.MM.y HH:mm").format(java.util.Date(item.timestamp)), modifier = Modifier.weight(2f))
                        Row {
                            IconButton(onClick = { editingReplacement = item; showDialog = true }) { Icon(Icons.Default.Edit, null) }
                            IconButton(onClick = { viewModel.deleteReplacement(item) }) { Icon(Icons.Default.Delete, null) }
                        }
                    }
                    Divider()
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showDialog) {
        ReplacementDialog(
            replacement = editingReplacement,
            onDismiss = { showDialog = false },
            onConfirm = { opora, roller, reason ->
                if (editingReplacement == null) {
                    viewModel.addReplacement(opora, roller, reason)
                } else {
                    viewModel.updateReplacement(editingReplacement!!.copy(opora = opora, roller = roller, reason = reason))
                }
                showDialog = false
            }
        )
    }
}

@Composable
fun androidx.compose.foundation.layout.RowScope.TableHeader(title: String, key: String, sortBy: String, ascending: Boolean, onClick: (String) -> Unit) {
    TextButton(onClick = { onClick(key) }, modifier = Modifier.weight(if (key=="opora") 1f else 3f)) {
        Text(title + if (sortBy==key) (if (ascending) " ↑" else " ↓") else "")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplacementDialog(
    replacement: Replacement?,
    onDismiss: () -> Unit,
    onConfirm: (Int, String, String) -> Unit
) {
    var opora by remember { mutableStateOf(replacement?.opora?.toString() ?: "") }
    var roller by remember { mutableStateOf(replacement?.roller ?: "Рабочий правый") }
    var reason by remember { mutableStateOf(replacement?.reason ?: "Клин") }

    val rollers = listOf("Рабочий правый", "Рабочий левый", "Рабочий центральный", "Поддерживающий правый", "Поддерживающий левый")
    val reasons = listOf("Клин", "Рассыпался подшипник", "Шум", "Сильный шум", "Другое")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (replacement == null) "Добавить замену" else "Редактировать замену") },
        text = {
            Column {
                TextField(
                    value = opora, 
                    onValueChange = { opora = it }, 
                    label = { Text("Опора") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                
                var rollerExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = rollerExpanded, onExpandedChange = { rollerExpanded = it }) {
                    TextField(value = roller, onValueChange = {}, readOnly = true, label = { Text("Ролик") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rollerExpanded) }, modifier = Modifier.menuAnchor())
                    ExposedDropdownMenu(expanded = rollerExpanded, onDismissRequest = { rollerExpanded = false }) {
                        rollers.forEach { r -> DropdownMenuItem(text = { Text(r) }, onClick = { roller = r; rollerExpanded = false }) }
                    }
                }

                var reasonExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = reasonExpanded, onExpandedChange = { reasonExpanded = it }) {
                    TextField(value = reason, onValueChange = {}, readOnly = true, label = { Text("Причина") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reasonExpanded) }, modifier = Modifier.menuAnchor())
                    ExposedDropdownMenu(expanded = reasonExpanded, onDismissRequest = { reasonExpanded = false }) {
                        reasons.forEach { r -> DropdownMenuItem(text = { Text(r) }, onClick = { reason = r; reasonExpanded = false }) }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(opora.toIntOrNull() ?: 0, roller, reason)
            }) { Text(if (replacement == null) "Добавить" else "Сохранить") }
        }
    )
}

@Composable
fun ReplacementCard(replacement: Replacement, onEdit: () -> Unit, onDelete: () -> Unit) {
    val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(replacement.timestamp))
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Опора: ${replacement.opora}", style = MaterialTheme.typography.titleMedium)
                Text(date, style = MaterialTheme.typography.bodySmall)
            }
            Text("Ролик: ${replacement.roller}")
            Text("Причина: ${replacement.reason}")
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}
