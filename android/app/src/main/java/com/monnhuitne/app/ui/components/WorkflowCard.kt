package com.monnhuitne.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.monnhuitne.app.data.WorkflowWithLastRun
import com.monnhuitne.app.ui.theme.SurfaceCard
import com.monnhuitne.app.ui.theme.TextMuted

@Composable
fun WorkflowCard(item: WorkflowWithLastRun, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(SurfaceCard, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(item.workflow.name, style = MaterialTheme.typography.titleMedium)
            StatusBadge(status = if (item.workflow.active) "success" else "inactive")
        }
        val lastExecution = item.lastExecution
        Text(
            text = if (lastExecution != null) {
                "Dernière exécution : ${lastExecution.status}"
            } else {
                "Aucune exécution"
            },
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
