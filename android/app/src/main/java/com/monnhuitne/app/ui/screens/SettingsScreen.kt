package com.monnhuitne.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.monnhuitne.app.data.N8nCredentials
import com.monnhuitne.app.data.SettingsStore
import com.monnhuitne.app.ui.theme.ErrorColor
import com.monnhuitne.app.ui.theme.TextMuted

@Composable
fun SettingsScreen(
    store: SettingsStore,
    existing: N8nCredentials?,
    onSaved: (N8nCredentials) -> Unit,
    onLoggedOut: (() -> Unit)? = null,
    onReset: (() -> Unit)? = null
) {
    var baseUrl by remember { mutableStateOf(existing?.baseUrl ?: "") }
    var apiKey by remember { mutableStateOf(existing?.apiKey ?: "") }
    var pin by remember { mutableStateOf("") }
    var pinConfirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var savedMessage by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Réglages", style = MaterialTheme.typography.headlineSmall)

        Text("Connexion n8n", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            label = { Text("URL de l'API n8n") },
            placeholder = { Text("https://n8n.example.com/api/v1") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "L'URL de votre instance n8n suivie de /api/v1. L'app l'appelle directement : " +
                "pas de backend intermédiaire nécessaire côté Android.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("Clé API n8n") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Dans n8n : avatar/nom (bas à gauche) → Settings → n8n API → Create an API key.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )

        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it },
            label = { Text(if (existing == null) "Créer un PIN (verrouillage de l'app)" else "Nouveau PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = pinConfirm,
            onValueChange = { pinConfirm = it },
            label = { Text("Confirmer le PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "4 chiffres minimum. Sert uniquement à verrouiller l'app sur cet appareil " +
                "(les données sont déjà chiffrées par le système Android) — en cas d'oubli, " +
                "pas de récupération possible, il faut réinitialiser.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )

        error?.let { Text(it, color = ErrorColor) }
        if (savedMessage) Text("Enregistré.", color = TextMuted)

        Button(onClick = {
            error = null
            when {
                baseUrl.isBlank() || apiKey.isBlank() -> error = "URL et clé API requises."
                pin.length < 4 -> error = "Le PIN doit faire au moins 4 chiffres."
                pin != pinConfirm -> error = "Les deux PIN ne correspondent pas."
                else -> {
                    store.save(baseUrl, apiKey, pin)
                    savedMessage = true
                    pin = ""
                    pinConfirm = ""
                    onSaved(N8nCredentials(baseUrl.trimEnd('/'), apiKey))
                }
            }
        }) {
            Text(if (existing == null) "Enregistrer" else "Mettre à jour")
        }

        if (existing != null) {
            onLoggedOut?.let {
                TextButton(onClick = it) { Text("Verrouiller l'app") }
            }
            onReset?.let {
                TextButton(onClick = it) { Text("Supprimer la configuration de cet appareil", color = ErrorColor) }
            }
        }
    }
}
