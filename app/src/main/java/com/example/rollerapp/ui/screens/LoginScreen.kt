package com.example.rollerapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.rollerapp.presentation.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(viewModel: MainViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
        Text("Вход в систему", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Пароль") }, modifier = Modifier.fillMaxWidth(), visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                loading = true
                scope.launch {
                    val result = viewModel.signIn(email, password) { }
                    // signIn uses callback; we'll rely on viewModel to set signed-in state and show snackbar via observation
                    loading = false
                }
            }, modifier = Modifier.weight(1f)) {
                Text("Войти")
            }
            OutlinedButton(onClick = {
                loading = true
                scope.launch {
                    val result = viewModel.register(email, password) { }
                    loading = false
                }
            }, modifier = Modifier.weight(1f)) { Text("Регистрация") }
        }

        Spacer(Modifier.height(16.dp))
        Text("Вы должны войти или зарегистрироваться, чтобы использовать приложение.")
    }
}
