package com.monnhuitne.app.data

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class N8nApiException(val status: Int, message: String) : Exception(message)

/**
 * Client HTTP direct vers l'API n8n. Contrairement à la PWA (WebView, donc
 * soumise à CORS), une app native n'a pas cette restriction : OkHttp fait de
 * simples requêtes serveur-à-serveur, sans passer par un backend proxy.
 */
class N8nApi(baseUrl: String, private val apiKey: String) {

    private val baseUrl = baseUrl.trimEnd('/')
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
    private val jsonMediaType = "application/json".toMediaType()

    private suspend fun get(path: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl$path")
            .addHeader("X-N8N-API-KEY", apiKey)
            .build()
        execute(request, path)
    }

    private suspend fun post(path: String, body: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl$path")
            .addHeader("X-N8N-API-KEY", apiKey)
            .post(body.toRequestBody(jsonMediaType))
            .build()
        execute(request, path)
    }

    private fun execute(request: Request, path: String): String {
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw N8nApiException(response.code, "n8n API ${response.code} sur $path: ${text.take(200)}")
            }
            return text
        }
    }

    suspend fun listWorkflows(): List<N8nWorkflow> {
        val all = mutableListOf<N8nWorkflow>()
        var cursor: String? = null
        do {
            val qs = if (cursor != null) "?cursor=$cursor" else ""
            val page = json.decodeFromString<N8nWorkflowListResponse>(get("/workflows$qs"))
            all += page.data
            cursor = page.nextCursor
        } while (cursor != null)
        return all
    }

    suspend fun getWorkflow(id: String): N8nWorkflow =
        json.decodeFromString(get("/workflows/$id"))

    suspend fun listExecutions(workflowId: String? = null, status: String? = null, limit: Int = 20): List<N8nExecution> {
        val params = mutableListOf("limit=$limit")
        workflowId?.let { params += "workflowId=$it" }
        status?.let { params += "status=$it" }
        val page = json.decodeFromString<N8nExecutionListResponse>(get("/executions?${params.joinToString("&")}"))
        return page.data
    }

    suspend fun getLastExecution(workflowId: String): N8nExecution? =
        listExecutions(workflowId = workflowId, limit = 1).firstOrNull()

    /** Exécute /workflows/{id}/execute (pour les workflows démarrés par un "Execute Workflow Trigger"). */
    suspend fun triggerExecute(workflowId: String, payload: Map<String, String>) {
        post("/workflows/$workflowId/execute", json.encodeToString(payload))
    }

    /** POST direct sur le webhook de prod n8n (pas sous /api/v1, cf n8nWebhookBaseUrl). */
    suspend fun triggerWebhook(webhookPath: String, payload: Map<String, String>) = withContext(Dispatchers.IO) {
        val webhookBase = baseUrl.removeSuffix("/api/v1")
        val request = Request.Builder()
            .url("$webhookBase/webhook/$webhookPath")
            .post(json.encodeToString(payload).toRequestBody(jsonMediaType))
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw N8nApiException(response.code, "Déclenchement webhook échoué (${response.code})")
            }
        }
    }

    /** Récupère une exécution complète et en extrait un résumé d'erreur lisible. */
    suspend fun getExecutionErrorSummary(executionId: String): ExecutionErrorSummary? {
        val raw = Json.parseToJsonElement(get("/executions/$executionId?includeData=true")).jsonObject
        val resultData = raw["data"]?.jsonObject?.get("resultData")?.jsonObject ?: return null
        val error = resultData["error"]?.jsonObject ?: return null
        val message = error["message"]?.jsonPrimitive?.contentOrNull ?: "Erreur inconnue"
        val nodeName = error["node"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull
            ?: resultData["lastNodeExecuted"]?.jsonPrimitive?.contentOrNull
            ?: "Node inconnu"
        val timestamp = raw["stoppedAt"]?.jsonPrimitive?.contentOrNull ?: ""
        return ExecutionErrorSummary(nodeName, humanizeErrorMessage(message), timestamp)
    }

    private fun humanizeErrorMessage(message: String): String =
        message.replace(Regex("^ERROR:\\s*", RegexOption.IGNORE_CASE), "")
            .lineSequence().first()
            .take(300)
}

/**
 * Détecte les champs d'entrée attendus par un "Execute Workflow Trigger" (schéma JSON simple).
 * Heuristique best-effort sur une forme JSON non garantie : toute erreur de forme
 * inattendue donne juste une liste vide plutôt que de faire planter l'écran.
 */
fun detectTriggerInputs(workflow: N8nWorkflow): List<TriggerInputField> = runCatching {
    val triggerNode = workflow.nodes.firstOrNull { it.type.contains("executeWorkflowTrigger") } ?: return@runCatching emptyList()
    val workflowInputs = triggerNode.parameters?.get("workflowInputs") as? JsonObject ?: return@runCatching emptyList()
    val values = workflowInputs["values"] as? JsonArray ?: return@runCatching emptyList()
    values.mapNotNull { entry ->
        val obj = entry as? JsonObject ?: return@mapNotNull null
        val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
        val type = obj["type"]?.jsonPrimitive?.contentOrNull ?: "string"
        TriggerInputField(key = name, label = name, type = type)
    }
}.getOrDefault(emptyList())

/** Cherche un node webhook pour savoir si le déclenchement doit passer par /webhook/... plutôt que /execute. */
fun findWebhookPath(workflow: N8nWorkflow): String? = runCatching {
    val webhookNode = workflow.nodes.firstOrNull { it.type.contains("webhook") } ?: return@runCatching null
    webhookNode.parameters?.get("path")?.jsonPrimitive?.contentOrNull
}.getOrNull()
