package com.medtrack.app.ui.screens.report

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.medtrack.app.domain.ocr.VisionSettings

@Composable
fun VisionConnectionDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val store = remember { VisionSettings(context) }
    var endpoint by remember { mutableStateOf(store.load()?.endpoint.orEmpty()) }
    var token by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("AI connection") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Optional Gemini reading through your private gateway. Provider API keys stay on the server. Your photos stay local unless you approve sending one.")
            OutlinedTextField(endpoint, { endpoint = it.take(500) }, label = { Text("Gateway URL · https://…/v1/extract") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(token, { token = it.take(512) }, label = { Text("Gateway access token") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Text("Leave the token blank to keep the saved token for this same URL. Setup instructions: vision-server/README.md in the project.", style = MaterialTheme.typography.bodySmall)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            OutlinedButton(onClick = { store.clear(); onDismiss() }) { Text("Disconnect AI") }
        }
    }, confirmButton = { Button(onClick = {
        runCatching {
            val old = store.load()
            store.save(endpoint.trim(), token.ifBlank { old?.takeIf { it.endpoint == endpoint.trim() }?.token.orEmpty() })
        }.onSuccess { onDismiss() }.onFailure { error = it.message ?: "Could not save connection." }
    }) { Text("Save connection") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
