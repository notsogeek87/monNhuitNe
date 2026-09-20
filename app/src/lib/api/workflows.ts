import { n8nRequest, type N8nClientConfig } from './client';
import type { N8nWorkflow, N8nWorkflowListResponse, TriggerInputField } from './types';

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
 * Déclenche un workflow. Deux modes selon comment le workflow démarre :
 * - `webhook`: POST direct sur l'URL de webhook production du workflow.
 * - `execute`: API n8n POST /workflows/{id}/execute avec un payload d'entrée
 *   (nécessite que le workflow soit conçu pour recevoir des données, ex un
 *   "Execute Workflow Trigger" ou un premier node "Set").
 */
export async function triggerWorkflow(
	config: N8nClientConfig,
	workflowId: string,
	options: { mode: 'webhook'; webhookPath: string; payload: Record<string, unknown> } | {
		mode: 'execute';
		payload: Record<string, unknown>;
	}
): Promise<{ executionId?: string }> {
	if (options.mode === 'webhook') {
		// Le webhook n'est pas sous /api/v1, il est proxifié séparément (cf server/src/proxy.ts, route /hooks).
		const res = await fetch(`${config.proxyBaseUrl.replace('/api/n8n', '')}/hooks/${options.webhookPath}`, {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify(options.payload)
		});
		if (!res.ok) throw new Error(`Déclenchement webhook échoué (${res.status})`);
		return {};
	}

	const result = await n8nRequest<{ executionId?: string }>(config, `/workflows/${workflowId}/execute`, {
		method: 'POST',
		body: JSON.stringify(options.payload)
	});
	return result;
}

/**
 * Détecte grossièrement les champs d'entrée attendus par un workflow en inspectant
 * son premier node déclencheur. Heuristique MVP : suffisant pour les workflows
 * "Execute Workflow Trigger" avec un schéma JSON simple déclaré en paramètres.
 */
export function detectTriggerInputs(workflow: N8nWorkflow): TriggerInputField[] {
	const triggerNode = workflow.nodes?.find((n) => n.type.includes('executeWorkflowTrigger'));
	const schema = triggerNode?.parameters?.workflowInputs as
		| { values?: { name: string; type?: string }[] }
		| undefined;

	if (!schema?.values) return [];

	return schema.values.map((field) => ({
		key: field.name,
		label: field.name,
		type: (field.type as TriggerInputField['type']) ?? 'string',
		required: true
	}));
}
