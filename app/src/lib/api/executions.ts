import { n8nRequest, type N8nClientConfig } from './client';
import type { ExecutionErrorSummary, N8nExecution, N8nExecutionListResponse } from './types';

export async function listExecutions(
	config: N8nClientConfig,
	options: { workflowId?: string; status?: 'error' | 'success'; limit?: number } = {}
): Promise<N8nExecution[]> {
	const params = new URLSearchParams();
	if (options.workflowId) params.set('workflowId', options.workflowId);
	if (options.status) params.set('status', options.status);
	params.set('limit', String(options.limit ?? 20));

	const page: N8nExecutionListResponse = await n8nRequest(config, `/executions?${params.toString()}`);
	return page.data;
}

export async function getLastExecution(
	config: N8nClientConfig,
	workflowId: string
): Promise<N8nExecution | undefined> {
	const [last] = await listExecutions(config, { workflowId, limit: 1 });
	return last;
}

/**
 * Récupère une exécution complète (avec `data` brut) et en extrait un résumé
 * d'erreur lisible, plutôt que d'exposer le JSON brut de n8n à l'écran.
 */
export async function getExecutionErrorSummary(
	config: N8nClientConfig,
	executionId: string
): Promise<ExecutionErrorSummary | undefined> {
	const raw = await n8nRequest<{
		stoppedAt: string | null;
		data?: {
			resultData?: {
				error?: { message?: string; node?: { name?: string } };
				lastNodeExecuted?: string;
			};
		};
	}>(config, `/executions/${executionId}?includeData=true`);

	const error = raw.data?.resultData?.error;
	if (!error) return undefined;

	return {
		nodeName: error.node?.name ?? raw.data?.resultData?.lastNodeExecuted ?? 'Node inconnu',
		message: humanizeErrorMessage(error.message ?? 'Erreur inconnue'),
		timestamp: raw.stoppedAt ?? new Date().toISOString()
	};
}

/** Nettoie les messages d'erreur techniques n8n pour rester lisibles sur mobile. */
function humanizeErrorMessage(message: string): string {
	return message
		.replace(/^ERROR:\s*/i, '')
		.split('\n')[0]
		.slice(0, 300);
}
