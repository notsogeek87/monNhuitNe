import { writable } from 'svelte/store';
import { defaultWorkflowFilters, type WorkflowFiltersState } from '../workflows/filtering';

// Préférences d'affichage (statut, tags, tri) persistées entre deux sessions ; la
// recherche texte, elle, ne l'est pas — on ne veut pas qu'une réouverture de l'app
// retombe sur une liste vide à cause d'une recherche oubliée.
const STORAGE_KEY = 'monnuitne.workflow-filters.v1';

function loadPersisted(): Partial<WorkflowFiltersState> {
	try {
		const raw = localStorage.getItem(STORAGE_KEY);
		return raw ? JSON.parse(raw) : {};
	} catch {
		return {};
	}
}

function createWorkflowFiltersStore() {
	const initial: WorkflowFiltersState = { ...defaultWorkflowFilters, ...loadPersisted(), query: '' };
	const store = writable<WorkflowFiltersState>(initial);

	store.subscribe((state) => {
		try {
			const { query: _query, ...toPersist } = state;
			localStorage.setItem(STORAGE_KEY, JSON.stringify(toPersist));
		} catch {
			// Stockage indisponible (navigation privée, quota dépassé...) : on continue sans persister.
		}
	});

	return store;
}

export const workflowFilters = createWorkflowFiltersStore();

export function resetWorkflowFilters(): void {
	workflowFilters.set({ ...defaultWorkflowFilters });
}
