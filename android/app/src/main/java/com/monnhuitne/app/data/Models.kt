package com.monnhuitne.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// Miroir Kotlin du strict nécessaire de l'API REST n8n (https://docs.n8n.io/api/),
// même périmètre que app/src/lib/api/types.ts côté PWA.

@Serializable
data class N8nWorkflowNode(
    val id: String = "",
    val name: String = "",
    val type: String = "",
    val parameters: JsonObject? = null
)

@Serializable
data class N8nWorkflow(
    val id: String,
    val name: String,
    val active: Boolean = false,
    val updatedAt: String = "",
    val createdAt: String = "",
    val nodes: List<N8nWorkflowNode> = emptyList()
)

@Serializable
data class N8nWorkflowListResponse(
    val data: List<N8nWorkflow>,
    val nextCursor: String? = null
)

@Serializable
data class N8nExecution(
    val id: String,
    val workflowId: String? = null,
    val finished: Boolean = false,
    val mode: String = "",
    val status: String = "unknown",
    val startedAt: String = "",
    val stoppedAt: String? = null
)

@Serializable
data class N8nExecutionListResponse(
    val data: List<N8nExecution>,
    val nextCursor: String? = null
)

data class WorkflowWithLastRun(
    val workflow: N8nWorkflow,
    val lastExecution: N8nExecution?
)

data class ExecutionErrorSummary(
    val nodeName: String,
    val message: String,
    val timestamp: String
)

data class StaleWorkflowInfo(
    val id: String,
    val name: String,
    val lastRunAt: String?,
    val daysSinceLastRun: Long
)

data class HealthSummary(
    val failuresLast24h: Int,
    val staleWorkflows: List<StaleWorkflowInfo>,
    val generatedAt: String
)
