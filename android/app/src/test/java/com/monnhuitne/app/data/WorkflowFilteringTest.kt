package com.monnhuitne.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

// Mêmes cas que app/src/lib/workflows/filtering.test.ts côté PWA.

private fun workflow(
    id: String,
    name: String,
    active: Boolean = true,
    tags: List<N8nWorkflowTag> = emptyList(),
    lastExecution: N8nExecution? = null
) = WorkflowWithLastRun(
    workflow = N8nWorkflow(id = id, name = name, active = active, createdAt = "2024-01-01T00:00:00.000Z", tags = tags),
    lastExecution = lastExecution
)

private val errorWf = workflow(
    id = "err",
    name = "Sync factures",
    tags = listOf(N8nWorkflowTag("t1", "compta")),
    lastExecution = N8nExecution(id = "e1", workflowId = "err", status = "error", startedAt = "2024-06-01T10:00:00.000Z")
)
private val successWf = workflow(
    id = "ok",
    name = "Backup nightly",
    tags = listOf(N8nWorkflowTag("t2", "infra")),
    lastExecution = N8nExecution(id = "e2", workflowId = "ok", status = "success", startedAt = "2024-06-02T10:00:00.000Z")
)
private val neverRunWf = workflow(id = "never", name = "Alerte stock", active = true)
private val inactiveWf = workflow(id = "inactive", name = "Ancien import", active = false)

private val allWorkflows = listOf(errorWf, successWf, neverRunWf, inactiveWf)

class WorkflowFilteringTest {

    @Test
    fun `matchesStatus accepte tout avec all`() {
        assertEquals(true, matchesStatus(inactiveWf, StatusFilter.ALL))
    }

    @Test
    fun `matchesStatus filtre les workflows en echec`() {
        assertEquals(true, matchesStatus(errorWf, StatusFilter.ERROR))
        assertEquals(false, matchesStatus(successWf, StatusFilter.ERROR))
    }

    @Test
    fun `matchesStatus filtre les workflows jamais executes`() {
        assertEquals(true, matchesStatus(neverRunWf, StatusFilter.NEVER))
        assertEquals(false, matchesStatus(successWf, StatusFilter.NEVER))
    }

    @Test
    fun `matchesQuery recherche insensible a la casse sur le nom et les tags`() {
        assertEquals(true, matchesQuery(errorWf, "FACTURES"))
        assertEquals(false, matchesQuery(errorWf, "backup"))
        assertEquals(true, matchesQuery(errorWf, "compta"))
        assertEquals(true, matchesQuery(errorWf, "   "))
    }

    @Test
    fun `matchesTags matche si au moins un tag correspond`() {
        assertEquals(true, matchesTags(errorWf, emptySet()))
        assertEquals(true, matchesTags(errorWf, setOf("compta", "infra")))
        assertEquals(false, matchesTags(successWf, setOf("compta")))
    }

    @Test
    fun `sortWorkflows trie par nom alphabetique`() {
        val sorted = sortWorkflows(allWorkflows, SortBy.NAME)
        assertEquals(listOf("Alerte stock", "Ancien import", "Backup nightly", "Sync factures"), sorted.map { it.workflow.name })
    }

    @Test
    fun `sortWorkflows trie par priorite`() {
        val sorted = sortWorkflows(allWorkflows, SortBy.PRIORITY)
        assertEquals(listOf("err", "never", "ok", "inactive"), sorted.map { it.workflow.id })
    }

    @Test
    fun `sortWorkflows trie par derniere execution`() {
        val sorted = sortWorkflows(allWorkflows, SortBy.LAST_RUN)
        assertEquals(listOf("ok", "err", "never", "inactive"), sorted.map { it.workflow.id })
    }

    @Test
    fun `computeStatusCounts compte chaque categorie independamment`() {
        val counts = computeStatusCounts(allWorkflows)
        assertEquals(StatusCounts(all = 4, active = 3, inactive = 1, error = 1, never = 2), counts)
    }

    @Test
    fun `collectTagNames renvoie les tags uniques tries`() {
        assertEquals(listOf("compta", "infra"), collectTagNames(allWorkflows))
    }

    @Test
    fun `filterAndSortWorkflows combine filtre statut et tri`() {
        val result = filterAndSortWorkflows(allWorkflows, WorkflowFiltersState(status = StatusFilter.ACTIVE, sort = SortBy.NAME))
        assertEquals(listOf("never", "ok", "err"), result.map { it.workflow.id })
    }
}
