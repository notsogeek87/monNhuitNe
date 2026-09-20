import { writable } from 'svelte/store';
import type { N8nClientConfig } from '../api/client';
import { getLastExecution } from '../api/executions';
import { listWorkflows } from '../api/workflows';
import type { WorkflowWithLastRun } from '../api/types';

interface WorkflowsState {
	items: WorkflowWithLastRun[];
	loading: boolean;
	error: string | null;
	lastRefreshedAt: string | null;
}

function createWorkflowsStore() {
	const store = writable<WorkflowsState>({ items: [], loading: false, error: null, lastRefreshedAt: null });

	async function refresh(config: N8nClientConfig): Promise<void> {
		store.update((s) => ({ ...s, loading: true, error: null }));

		try {
			const workflows = await listWorkflows(config);

			// Dernière exécution récupérée en parallèle, workflow par workflow.
			const withLastRun: WorkflowWithLastRun[] = await Promise.all(
				workflows.map(async (wf) => ({
					...wf,
					lastExecution: await getLastExecution(config, wf.id).catch(() => undefined)
				}))
			);

			withLastRun.sort((a, b) => a.name.localeCompare(b.name));

			store.set({ items: withLastRun, loading: false, error: null, lastRefreshedAt: new Date().toISOString() });
		} catch (err) {
			store.update((s) => ({ ...s, loading: false, error: (err as Error).message }));
		}
	}

	return { ...store, refresh };
}

export const workflowsStore = createWorkflowsStore();
