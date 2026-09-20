import { n8nRequest, type N8nClientConfig } from './client';
import type { N8nWorkflow, N8nWorkflowListResponse } from './types';

export async function listWorkflows(config: N8nClientConfig): Promise<N8nWorkflow[]> {
	const all: N8nWorkflow[] = [];
	let cursor: string | null = null;

	let hasMore = true;

	while (hasMore) {
		const qs: string = cursor ? `?cursor=${encodeURIComponent(cursor)}` : '';
		const page: N8nWorkflowListResponse = await n8nRequest(config, `/workflows${qs}`);
		all.push(...page.data);
		cursor = page.nextCursor;
		hasMore = cursor !== null;
	}

	return all;
}

export async function getWorkflow(config: N8nClientConfig, id: string): Promise<N8nWorkflow> {
	return n8nRequest(config, `/workflows/${id}`);
}

/**
 * Déclenche un workflow via son webhook de production. L'API REST publique de
 * n8n (`/api/v1/...`) est volontairement limitée à la gestion des workflows
 * (lister, activer/désactiver...) : elle n'expose aucune route générique pour
 * lancer une exécution à la demande (`POST /workflows/{id}/execute` n'existe
 * pas et répond 405). Le seul déclenchement externe possible est donc un
 * webhook — voir `findWebhookPath`, qui doit renvoyer un chemin non nul avant
 * d'appeler cette fonction.
 */
export async function triggerWorkflow(
	config: N8nClientConfig,
	webhookPath: string,
	payload: Record<string, unknown>
): Promise<void> {
	// Le webhook n'est pas sous /api/v1, il est proxifié séparément (cf server/src/proxy.ts, route /hooks).
	const res = await fetch(`${config.proxyBaseUrl.replace('/api/n8n', '')}/hooks/${webhookPath}`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify(payload)
	});
	if (!res.ok) throw new Error(`Déclenchement webhook échoué (${res.status})`);
}

/** Cherche un node Webhook pour savoir si ce workflow peut être déclenché depuis l'app. */
export function findWebhookPath(workflow: N8nWorkflow): string | null {
	const webhookNode = workflow.nodes?.find((n) => n.type.includes('webhook'));
	return (webhookNode?.parameters?.path as string) ?? null;
}
