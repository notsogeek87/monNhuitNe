package com.monnhuitne.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.monnhuitne.app.data.HealthSummary
import com.monnhuitne.app.data.N8nApi
import com.monnhuitne.app.data.computeHealthSummary
import com.monnhuitne.app.ui.theme.ErrorColor
import com.monnhuitne.app.ui.theme.TextMuted
import com.monnhuitne.app.ui.theme.Warning

@Composable
fun HealthScreen(api: N8nApi) {
    var summary by remember { mutableStateOf<HealthSummary?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(api) {
        loading = true
        error = null
        try {
            summary = computeHealthSummary(api)
        } catch (err: Exception) {
            error = err.message ?: "Erreur inconnue"
        } finally {
            loading = false
        }
    }

    when {
        loading -> CenteredSpinner()
        error != null -> CenteredMessage(error!!, ErrorColor)
        summary != null -> {
            val s = summary!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text("Santé globale", style = MaterialTheme.typography.headlineSmall)

                Text(
                    "Échecs (24h) : ${s.failuresLast24h}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (s.failuresLast24h > 0) ErrorColor else TextMuted,
                    modifier = Modifier.padding(top = 16.dp)
                )

                Text(
                    "Workflows inactifs depuis longtemps",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                )
                if (s.staleWorkflows.isEmpty()) {
                    Text("Aucun.", color = TextMuted)
                } else {
                    s.staleWorkflows.forEach { stale ->
                        Text(
                            "${stale.name} — ${if (stale.daysSinceLastRun == Long.MAX_VALUE) "jamais exécuté" else "${stale.daysSinceLastRun} j"}",
                            color = Warning,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
