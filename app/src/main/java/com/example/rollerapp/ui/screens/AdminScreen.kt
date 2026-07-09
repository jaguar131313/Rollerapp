package com.example.rollerapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.rollerapp.presentation.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun AdminScreen(viewModel: MainViewModel, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    var processing by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Админ-панель Firebase", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text("Операции с базой данных в облаке (только для админ-пользователей).")
        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            processing = true
            scope.launch {
                val res = viewModel.clearRemoteData()
                processing = false
                snackbar.showSnackbar(if (res.isSuccess) "Удалено всё удалённое содержимое" else "Ошибка: ${res.exceptionOrNull()?.message}")
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Очистить все данные в Firebase")
        }

        Spacer(Modifier.height(8.dp))

        Button(onClick = {
            processing = true
            scope.launch {
                val res = viewModel.removeRemoteDuplicates()
                processing = false
                snackbar.showSnackbar(if (res.isSuccess) "Дубликаты удалены" else "Ошибка: ${res.exceptionOrNull()?.message}")
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Удалить дубликаты в Firebase")
        }

        Spacer(Modifier.height(8.dp))

        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text("Закрыть")
        }

        Spacer(Modifier.height(16.dp))
        SnackbarHost(hostState = snackbar)
    }
}
