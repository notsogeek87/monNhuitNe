package com.monnhuitne.app.data

import java.time.Instant

// Logique pure de tri/filtrage de la liste des workflows, sans dépendance Compose —
// même approche et mêmes règles que app/src/lib/workflows/filtering.ts côté PWA,
// pour un comportement identique sur les deux plateformes.

enum class StatusFilter { ALL, ACTIVE, INACTIVE, ERROR, NEVER }
enum class SortBy { PRIORITY, NAME, LAST_RUN }

data class WorkflowFiltersState(
    val query: String = "",
    val status: StatusFilter = StatusFilter.ALL,
    val tags: Set<String> = emptySet(),
    val sort: SortBy = SortBy.PRIORITY
)

fun matchesStatus(item: WorkflowWithLastRun, status: StatusFilter): Boolean = when (status) {
    StatusFilter.ALL -> true
    StatusFilter.ACTIVE -> item.workflow.active
    StatusFilter.INACTIVE -> !item.workflow.active
    StatusFilter.ERROR -> item.lastExecution?.status == "error"
    StatusFilter.NEVER -> item.lastExecution == null
}

fun matchesQuery(item: WorkflowWithLastRun, query: String): Boolean {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return true
    if (item.workflow.name.lowercase().contains(q)) return true
    return item.workflow.tags.any { it.name.lowercase().contains(q) }
}

fun matchesTags(item: WorkflowWithLastRun, tags: Set<String>): Boolean {
    if (tags.isEmpty()) return true
    val workflowTags = item.workflow.tags.map { it.name }.toSet()
    return tags.any { it in workflowTags }
}

/**
 * Rang d'urgence pour le tri "Priorité" : les workflows qui ont besoin d'attention
 * remontent en premier (échec > en cours > en attente > jamais exécuté alors qu'actif),
 * puis les workflows sains, puis les workflows désactivés en dernier.
 */
private fun priorityRank(item: WorkflowWithLastRun): Int = when {
    item.lastExecution?.status == "error" -> 0
    item.lastExecution?.status == "running" -> 1
    item.lastExecution?.status == "waiting" -> 2
    item.lastExecution == null && item.workflow.active -> 3
    !item.workflow.active -> 5
    else -> 4
}

private fun lastRunEpochMillis(item: WorkflowWithLastRun): Long {
    val startedAt = item.lastExecution?.startedAt.orEmpty()
    if (startedAt.isEmpty()) return Long.MIN_VALUE
    return runCatching { Instant.parse(startedAt).toEpochMilli() }.getOrDefault(Long.MIN_VALUE)
}

fun sortWorkflows(items: List<WorkflowWithLastRun>, sort: SortBy): List<WorkflowWithLastRun> = when (sort) {
    SortBy.NAME -> items.sortedBy { it.workflow.name.lowercase() }
    SortBy.LAST_RUN -> items.sortedWith(
        compareByDescending<WorkflowWithLastRun> { lastRunEpochMillis(it) }.thenBy { it.workflow.name.lowercase() }
    )
    SortBy.PRIORITY -> items.sortedWith(
        compareBy<WorkflowWithLastRun> { priorityRank(it) }.thenBy { it.workflow.name.lowercase() }
    )
}

fun filterWorkflows(items: List<WorkflowWithLastRun>, filters: WorkflowFiltersState): List<WorkflowWithLastRun> =
    items.filter {
        matchesStatus(it, filters.status) && matchesQuery(it, filters.query) && matchesTags(it, filters.tags)
    }

fun filterAndSortWorkflows(
    items: List<WorkflowWithLastRun>,
    filters: WorkflowFiltersState
): List<WorkflowWithLastRun> = sortWorkflows(filterWorkflows(items, filters), filters.sort)

data class StatusCounts(
    val all: Int,
    val active: Int,
    val inactive: Int,
    val error: Int,
    val never: Int
)

fun computeStatusCounts(items: List<WorkflowWithLastRun>): StatusCounts = StatusCounts(
    all = items.size,
    active = items.count { it.workflow.active },
    inactive = items.count { !it.workflow.active },
    error = items.count { it.lastExecution?.status == "error" },
    never = items.count { it.lastExecution == null }
)

fun countFor(counts: StatusCounts, status: StatusFilter): Int = when (status) {
    StatusFilter.ALL -> counts.all
    StatusFilter.ACTIVE -> counts.active
    StatusFilter.INACTIVE -> counts.inactive
    StatusFilter.ERROR -> counts.error
    StatusFilter.NEVER -> counts.never
}

fun collectTagNames(items: List<WorkflowWithLastRun>): List<String> =
    items.flatMap { it.workflow.tags.map { tag -> tag.name } }.toSortedSet().toList()
