package com.monnhuitne.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.monnhuitne.app.data.ExecutionErrorSummary
import com.monnhuitne.app.data.N8nApi
import com.monnhuitne.app.data.N8nExecution
import com.monnhuitne.app.data.N8nWorkflow
import com.monnhuitne.app.data.detectTriggerInputs
import com.monnhuitne.app.data.findWebhookPath
import com.monnhuitne.app.ui.components.StatusBadge
import com.monnhuitne.app.ui.theme.ErrorColor
import com.monnhuitne.app.ui.theme.TextMuted
import kotlinx.coroutines.launch

@Composable
fun WorkflowDetailScreen(api: N8nApi, workflowId: String, onBack: () -> Unit) {
    var workflow by remember { mutableStateOf<N8nWorkflow?>(null) }
    var executions by remember { mutableStateOf<List<N8nExecution>>(emptyList()) }
    var selectedError by remember { mutableStateOf<ExecutionErrorSummary?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var triggering by remember { mutableStateOf(false) }
    var triggerFeedback by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        loading = true
        error = null
        try {
            workflow = api.getWorkflow(workflowId)
            executions = api.listExecutions(workflowId = workflowId, limit = 20)
        } catch (err: Exception) {
            error = err.message ?: "Erreur inconnue"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(workflowId) { load() }

    when {
        loading -> CenteredSpinner()
        error != null -> CenteredMessage(error!!, ErrorColor)
        workflow != null -> {
            val wf = workflow!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                TextButton(onClick = onBack) { Text("← Workflows") }
                Text(wf.name, style = MaterialTheme.typography.headlineSmall)
                StatusBadge(status = if (wf.active) "success" else "inactive")

                Text("Déclencher", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 20.dp))
                Button(
                    enabled = !triggering,
                    onClick = {
                        scope.launch {
                            triggering = true
                            triggerFeedback = null
                            try {
                                val webhookPath = findWebhookPath(wf)
                                val payload = emptyMap<String, String>()
                                if (webhookPath != null) {
                                    api.triggerWebhook(webhookPath, payload)
                                } else {
                                    api.triggerExecute(wf.id, payload)
                                }
                                triggerFeedback = "Déclenché. Rafraîchissement…"
                                load()
                            } catch (err: Exception) {
                                triggerFeedback = "Échec du déclenchement : ${err.message}"
                            } finally {
                                triggering = false
                            }
                        }
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(if (triggering) "Déclenchement…" else "Déclencher ce workflow")
                }
                if (detectTriggerInputs(wf).isNotEmpty()) {
                    Text(
                        "Ce workflow attend des paramètres d'entrée — non pris en charge pour l'instant, " +
                            "il sera déclenché sans payload.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                triggerFeedback?.let { Text(it, color = TextMuted, modifier = Modifier.padding(top = 6.dp)) }

                Text(
                    "Exécutions récentes",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                )
                if (executions.isEmpty()) {
                    Text("Aucune exécution.", color = TextMuted)
                } else {
                    executions.forEach { execution ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        selectedError = if (execution.status == "error") {
                                            runCatching { api.getExecutionErrorSummary(execution.id) }.getOrNull()
                                        } else {
                                            null
                                        }
                                    }
                                }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(execution.startedAt, color = TextMuted, style = MaterialTheme.typography.bodySmall)
                            StatusBadge(status = execution.status)
                        }
                    }
                }

                selectedError?.let { err ->
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Text("Node : ${err.nodeName}", style = MaterialTheme.typography.bodySmall)
                        Text(err.message, color = ErrorColor, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
