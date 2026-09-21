// Logique pure de tri/filtrage de la liste des workflows (aucune dépendance Svelte),
// testable en isolation — cf lib/api/workflows.test.ts pour la même approche côté API.
import type { WorkflowWithLastRun } from '../api/types';

export type StatusFilter = 'all' | 'active' | 'inactive' | 'error' | 'never';
export type SortBy = 'priority' | 'name' | 'lastRun';

export interface WorkflowFiltersState {
	query: string;
	status: StatusFilter;
	tags: string[];
	sort: SortBy;
}

export const defaultWorkflowFilters: WorkflowFiltersState = {
	query: '',
	status: 'all',
	tags: [],
	sort: 'priority'
};

const STATUS_PREDICATES: Record<Exclude<StatusFilter, 'all'>, (wf: WorkflowWithLastRun) => boolean> = {
	active: (wf) => wf.active,
	inactive: (wf) => !wf.active,
	error: (wf) => wf.lastExecution?.status === 'error',
	never: (wf) => !wf.lastExecution
};

export function matchesStatus(wf: WorkflowWithLastRun, status: StatusFilter): boolean {
	return status === 'all' || STATUS_PREDICATES[status](wf);
}

export function matchesQuery(wf: WorkflowWithLastRun, query: string): boolean {
	const q = query.trim().toLowerCase();
	if (!q) return true;
	if (wf.name.toLowerCase().includes(q)) return true;
	return (wf.tags ?? []).some((tag) => tag.name.toLowerCase().includes(q));
}

export function matchesTags(wf: WorkflowWithLastRun, tags: string[]): boolean {
	if (tags.length === 0) return true;
	const wfTags = new Set((wf.tags ?? []).map((t) => t.name));
	return tags.some((tag) => wfTags.has(tag));
}

/**
 * Rang d'urgence pour le tri "Priorité" : les workflows qui ont besoin d'attention
 * remontent en premier (échec > en cours > en attente > jamais exécuté alors qu'actif),
 * puis les workflows sains, puis les workflows désactivés en dernier.
 */
function priorityRank(wf: WorkflowWithLastRun): number {
	if (wf.lastExecution?.status === 'error') return 0;
	if (wf.lastExecution?.status === 'running') return 1;
	if (wf.lastExecution?.status === 'waiting') return 2;
	if (!wf.lastExecution && wf.active) return 3;
	if (!wf.active) return 5;
	return 4;
}

function lastRunTime(wf: WorkflowWithLastRun): number {
	return wf.lastExecution ? new Date(wf.lastExecution.startedAt).getTime() : -Infinity;
}

export function sortWorkflows(items: WorkflowWithLastRun[], sort: SortBy): WorkflowWithLastRun[] {
	const sorted = [...items];
	switch (sort) {
		case 'name':
			sorted.sort((a, b) => a.name.localeCompare(b.name));
			break;
		case 'lastRun':
			sorted.sort((a, b) => lastRunTime(b) - lastRunTime(a) || a.name.localeCompare(b.name));
			break;
		case 'priority':
		default:
			sorted.sort((a, b) => priorityRank(a) - priorityRank(b) || a.name.localeCompare(b.name));
			break;
	}
	return sorted;
}

export function filterWorkflows(
	items: WorkflowWithLastRun[],
	filters: Pick<WorkflowFiltersState, 'status' | 'query' | 'tags'>
): WorkflowWithLastRun[] {
	return items.filter(
		(wf) => matchesStatus(wf, filters.status) && matchesQuery(wf, filters.query) && matchesTags(wf, filters.tags)
	);
}

export function filterAndSortWorkflows(
	items: WorkflowWithLastRun[],
	filters: WorkflowFiltersState
): WorkflowWithLastRun[] {
	return sortWorkflows(filterWorkflows(items, filters), filters.sort);
}

export interface StatusCounts {
	all: number;
	active: number;
	inactive: number;
	error: number;
	never: number;
}

export function computeStatusCounts(items: WorkflowWithLastRun[]): StatusCounts {
	return {
		all: items.length,
		active: items.filter((wf) => wf.active).length,
		inactive: items.filter((wf) => !wf.active).length,
		error: items.filter((wf) => wf.lastExecution?.status === 'error').length,
		never: items.filter((wf) => !wf.lastExecution).length
	};
}

export function collectTagNames(items: WorkflowWithLastRun[]): string[] {
	const set = new Set<string>();
	for (const wf of items) {
		for (const tag of wf.tags ?? []) set.add(tag.name);
	}
	return [...set].sort((a, b) => a.localeCompare(b));
}
