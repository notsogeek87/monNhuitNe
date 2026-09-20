package com.monnhuitne.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.monnhuitne.app.data.N8nApi
import com.monnhuitne.app.data.WorkflowWithLastRun
import com.monnhuitne.app.ui.components.WorkflowCard
import com.monnhuitne.app.ui.theme.ErrorColor
import com.monnhuitne.app.ui.theme.TextMuted
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@Composable
fun WorkflowsScreen(api: N8nApi, onOpenWorkflow: (String) -> Unit) {
    var workflowItems by remember { mutableStateOf<List<WorkflowWithLastRun>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(api) {
        loading = true
        error = null
        try {
            val workflows = api.listWorkflows()
            workflowItems = coroutineScope {
                workflows.map { wf ->
                    async { WorkflowWithLastRun(wf, runCatching { api.getLastExecution(wf.id) }.getOrNull()) }
                }.map { it.await() }
            }.sortedBy { it.workflow.name.lowercase() }
        } catch (err: Exception) {
            error = err.message ?: "Erreur inconnue"
        } finally {
            loading = false
        }
    }

    when {
        loading -> CenteredSpinner()
        error != null -> CenteredMessage(error!!, ErrorColor)
        workflowItems.isEmpty() -> CenteredMessage("Aucun workflow trouvé.", TextMuted)
        else -> LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(workflowItems, key = { it.workflow.id }) { item ->
                WorkflowCard(item = item, onClick = { onOpenWorkflow(item.workflow.id) })
            }
        }
    }
}

@Composable
internal fun CenteredMessage(text: String, color: androidx.compose.ui.graphics.Color) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = color)
    }
}

@Composable
internal fun CenteredSpinner() {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
