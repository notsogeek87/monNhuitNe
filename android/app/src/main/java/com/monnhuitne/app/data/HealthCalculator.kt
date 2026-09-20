package com.monnhuitne.app.data

import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Portage direct de server/src/healthPoll.ts::computeHealthSummary, calculé
 * ici sur l'appareil (plus de backend pour l'app native) à partir des mêmes
 * appels n8n.
 */
suspend fun computeHealthSummary(api: N8nApi, staleWorkflowDays: Int = 7): HealthSummary {
    val since24h = Instant.now().minus(24, ChronoUnit.HOURS)

    val workflows = api.listWorkflows()
    val recentFailures = api.listExecutions(status = "error", limit = 100)
        .filter { execution ->
            runCatching { Instant.parse(execution.startedAt) }.getOrNull()?.isAfter(since24h) == true
        }

    val staleWorkflows = workflows.filter { it.active }.mapNotNull { workflow ->
        val last = runCatching { api.getLastExecution(workflow.id) }.getOrNull()
        val lastRunAt = last?.startedAt
        val daysSinceLastRun = lastRunAt
            ?.let { runCatching { ChronoUnit.DAYS.between(Instant.parse(it), Instant.now()) }.getOrNull() }
            ?: Long.MAX_VALUE

        if (daysSinceLastRun >= staleWorkflowDays) {
            StaleWorkflowInfo(workflow.id, workflow.name, lastRunAt, daysSinceLastRun)
        } else {
            null
        }
    }

    return HealthSummary(
        failuresLast24h = recentFailures.size,
        staleWorkflows = staleWorkflows,
        generatedAt = Instant.now().toString()
    )
}
