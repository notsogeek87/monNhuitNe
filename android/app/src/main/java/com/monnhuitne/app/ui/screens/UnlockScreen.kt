package com.monnhuitne.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.monnhuitne.app.data.SettingsStore
import com.monnhuitne.app.ui.theme.ErrorColor

@Composable
fun UnlockScreen(store: SettingsStore, onUnlocked: () -> Unit, onReset: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("monNhuitNe verrouillé", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it; error = null },
            label = { Text("PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )
        error?.let { Text(it, color = ErrorColor) }
        Button(
            onClick = {
                if (store.verifyPin(pin)) onUnlocked() else error = "PIN incorrect."
            },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        ) {
            Text("Déverrouiller")
        }
        TextButton(onClick = onReset, modifier = Modifier.padding(top = 8.dp)) {
            Text("PIN oublié ? Réinitialiser la configuration", color = ErrorColor)
        }
    }
}
