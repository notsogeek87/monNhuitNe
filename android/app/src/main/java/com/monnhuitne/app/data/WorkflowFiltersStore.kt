package com.monnhuitne.app.data

import android.content.Context

/**
 * Préférences d'affichage (statut, tags, tri) persistées entre deux sessions ; la
 * recherche texte n'est pas persistée, pour éviter qu'une réouverture de l'app
 * retombe sur une liste vide à cause d'une recherche oubliée — même choix que
 * lib/stores/workflowFilters.ts côté PWA. Préférences UI non sensibles : pas besoin
 * du chiffrement d'EncryptedSharedPreferences utilisé par SettingsStore.
 */
class WorkflowFiltersStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): WorkflowFiltersState {
        val status = runCatching { StatusFilter.valueOf(prefs.getString(KEY_STATUS, null).orEmpty()) }
            .getOrDefault(StatusFilter.ALL)
        val sort = runCatching { SortBy.valueOf(prefs.getString(KEY_SORT, null).orEmpty()) }
            .getOrDefault(SortBy.PRIORITY)
        val tags = prefs.getStringSet(KEY_TAGS, emptySet()).orEmpty().toSet()
        return WorkflowFiltersState(query = "", status = status, tags = tags, sort = sort)
    }

    fun save(filters: WorkflowFiltersState) {
        prefs.edit()
            .putString(KEY_STATUS, filters.status.name)
            .putString(KEY_SORT, filters.sort.name)
            .putStringSet(KEY_TAGS, filters.tags)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "monnhuitne_workflow_filters"
        const val KEY_STATUS = "status"
        const val KEY_SORT = "sort"
        const val KEY_TAGS = "tags"
    }
}
