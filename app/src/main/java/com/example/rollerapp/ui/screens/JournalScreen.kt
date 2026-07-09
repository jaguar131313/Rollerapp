package com.example.rollerapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.rollerapp.presentation.MainViewModel
import com.example.rollerapp.data.BeltJournal
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun JournalScreen(viewModel: MainViewModel) {
    val entries by viewModel.journal.collectAsState()
    var text by remember { mutableStateOf("") }
    var editingEntry by remember { mutableStateOf<BeltJournal?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        TextField(
            value = if (editingEntry != null) editingEntry!!.text else text,
            onValueChange = { 
                if (editingEntry != null) editingEntry = editingEntry!!.copy(text = it) else text = it 
            },
            label = { Text(if (editingEntry != null) "Редактировать заметку" else "Заметка по ленте") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { 
                    if (editingEntry != null) {
                        viewModel.updateJournalEntry(editingEntry!!)
                        editingEntry = null
                    } else if (text.isNotBlank()) {
                        viewModel.addJournalEntry(text)
                        text = ""
                    }
                },
                modifier = Modifier.weight(1f).padding(vertical = 4.dp)
            ) {
                Icon(if (editingEntry != null) Icons.Default.Edit else Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text(if (editingEntry != null) "Сохранить" else "Добавить запись")
            }
            if (editingEntry != null) {
                OutlinedButton(
                    onClick = { editingEntry = null },
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text("Отмена")
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn {
            items(entries) { item ->
                JournalCard(
                    item, 
                    onEdit = { editingEntry = item },
                    onDelete = { viewModel.deleteJournalEntry(item) }
                )
            }
        }
    }
}

@Composable
fun JournalCard(entry: BeltJournal, onEdit: () -> Unit, onDelete: () -> Unit) {
    val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(entry.timestamp))
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(date, style = MaterialTheme.typography.bodySmall)
                Row {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(16.dp)) }
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)) }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(entry.text)
        }
    }
}
