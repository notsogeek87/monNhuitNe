package com.monnhuitne.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.monnhuitne.app.data.N8nApi
import com.monnhuitne.app.data.WorkflowFiltersState
import com.monnhuitne.app.data.WorkflowFiltersStore
import com.monnhuitne.app.data.WorkflowWithLastRun
import com.monnhuitne.app.data.filterAndSortWorkflows
import com.monnhuitne.app.ui.components.WorkflowCard
import com.monnhuitne.app.ui.components.WorkflowToolbar
import com.monnhuitne.app.ui.theme.ErrorColor
import com.monnhuitne.app.ui.theme.SurfaceCard
import com.monnhuitne.app.ui.theme.TextMuted
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@Composable
fun WorkflowsScreen(api: N8nApi, onOpenWorkflow: (String) -> Unit) {
    val context = LocalContext.current
    val filtersStore = remember { WorkflowFiltersStore(context) }
    var filters by remember { mutableStateOf(filtersStore.load()) }

    fun updateFilters(update: (WorkflowFiltersState) -> WorkflowFiltersState) {
        filters = update(filters)
        filtersStore.save(filters)
    }

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

    val filteredWorkflows = remember(workflowItems, filters) { filterAndSortWorkflows(workflowItems, filters) }

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
            item {
                WorkflowToolbar(
                    items = workflowItems,
                    filters = filters,
                    onQueryChange = { q -> updateFilters { it.copy(query = q) } },
                    onStatusChange = { s -> updateFilters { it.copy(status = s) } },
                    onToggleTag = { tag ->
                        updateFilters { f -> f.copy(tags = if (tag in f.tags) f.tags - tag else f.tags + tag) }
                    },
                    onSortChange = { s -> updateFilters { it.copy(sort = s) } }
                )
            }

            if (filteredWorkflows.isEmpty()) {
                item {
                    EmptyFilteredState(onReset = { updateFilters { WorkflowFiltersState() } })
                }
            } else {
                item {
                    Text(
                        text = resultCountLabel(filteredWorkflows.size, workflowItems.size),
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                items(filteredWorkflows, key = { it.workflow.id }) { item ->
                    WorkflowCard(item = item, onClick = { onOpenWorkflow(item.workflow.id) })
                }
            }
        }
    }
}

private fun resultCountLabel(shown: Int, total: Int): String {
    val plural = if (shown > 1) "s" else ""
    return if (shown != total) "$shown workflow$plural sur $total" else "$shown workflow$plural"
}

@Composable
private fun EmptyFilteredState(onReset: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCard, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Aucun workflow ne correspond à ces filtres.", color = TextMuted, style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = onReset) { Text("Réinitialiser les filtres") }
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
