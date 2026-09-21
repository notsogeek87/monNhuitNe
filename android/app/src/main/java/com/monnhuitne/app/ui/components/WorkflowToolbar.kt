package com.monnhuitne.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monnhuitne.app.data.SortBy
import com.monnhuitne.app.data.StatusFilter
import com.monnhuitne.app.data.WorkflowFiltersState
import com.monnhuitne.app.data.WorkflowWithLastRun
import com.monnhuitne.app.data.collectTagNames
import com.monnhuitne.app.data.computeStatusCounts
import com.monnhuitne.app.data.countFor
import com.monnhuitne.app.ui.theme.Accent
import com.monnhuitne.app.ui.theme.Border
import com.monnhuitne.app.ui.theme.ErrorColor
import com.monnhuitne.app.ui.theme.SurfaceCard
import com.monnhuitne.app.ui.theme.SurfaceElevated
import com.monnhuitne.app.ui.theme.TextMuted
import com.monnhuitne.app.ui.theme.TextPrimary

private val statusOptions = listOf(
    StatusFilter.ALL to "Tous",
    StatusFilter.ERROR to "En échec",
    StatusFilter.ACTIVE to "Actifs",
    StatusFilter.INACTIVE to "Inactifs",
    StatusFilter.NEVER to "Jamais exécuté"
)

private fun sortLabel(sort: SortBy): String = when (sort) {
    SortBy.PRIORITY -> "Priorité (problèmes d'abord)"
    SortBy.NAME -> "Nom (A → Z)"
    SortBy.LAST_RUN -> "Dernière exécution"
}

@Composable
fun WorkflowToolbar(
    items: List<WorkflowWithLastRun>,
    filters: WorkflowFiltersState,
    onQueryChange: (String) -> Unit,
    onStatusChange: (StatusFilter) -> Unit,
    onToggleTag: (String) -> Unit,
    onSortChange: (SortBy) -> Unit
) {
    val counts = remember(items) { computeStatusCounts(items) }
    val tagNames = remember(items) { collectTagNames(items) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = filters.query,
            onValueChange = onQueryChange,
            singleLine = true,
            placeholder = { Text("Rechercher un workflow ou un tag…") },
            leadingIcon = { Text("🔍") },
            trailingIcon = {
                if (filters.query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Text("×", fontSize = 18.sp, color = TextMuted)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            statusOptions.forEach { (status, label) ->
                FilterChipPill(
                    label = label,
                    count = countFor(counts, status),
                    selected = filters.status == status,
                    danger = status == StatusFilter.ERROR && counts.error > 0,
                    onClick = { onStatusChange(status) }
                )
            }
        }

        if (tagNames.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                tagNames.forEach { tag ->
                    FilterChipPill(
                        label = tag,
                        count = null,
                        selected = tag in filters.tags,
                        onClick = { onToggleTag(tag) }
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            SortControl(sort = filters.sort, onSortChange = onSortChange)
        }
    }
}

@Composable
private fun SortControl(sort: SortBy, onSortChange: (SortBy) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text("Trier : ${sortLabel(sort)} ▾", color = TextMuted, fontSize = 13.sp)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SortBy.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(sortLabel(option)) },
                    onClick = {
                        onSortChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun FilterChipPill(
    label: String,
    count: Int?,
    selected: Boolean,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val borderColor = when {
        danger && selected -> ErrorColor
        selected -> Accent
        else -> Border
    }
    val backgroundColor = when {
        danger && selected -> ErrorColor.copy(alpha = 0.20f)
        selected -> Accent.copy(alpha = 0.18f)
        else -> SurfaceCard
    }
    val textColor = if (selected) TextPrimary else TextMuted

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(label, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        if (count != null) {
            Text(
                text = count.toString(),
                color = textColor,
                fontSize = 11.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(SurfaceElevated)
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            )
        }
    }
}
