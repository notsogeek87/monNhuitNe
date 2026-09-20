import { config } from './config.js';

export interface N8nWorkflowLite {
	id: string;
	name: string;
	active: boolean;
	updatedAt: string;
}

export interface N8nExecutionLite {
	id: string;
	workflowId: string;
	status: string;
	startedAt: string;
	stoppedAt: string | null;
}

async function n8nFetch<T>(apiKey: string, path: string): Promise<T> {
	const res = await fetch(`${config.n8nBaseUrl}${path}`, {
		headers: { 'X-N8N-API-KEY': apiKey }
	});
	if (!res.ok) throw new Error(`n8n API ${res.status} sur ${path}`);
	return res.json() as Promise<T>;
}

export async function fetchAllWorkflows(apiKey: string): Promise<N8nWorkflowLite[]> {
	const all: N8nWorkflowLite[] = [];
	let cursor: string | null = null;
	let hasMore = true;

	while (hasMore) {
		const qs: string = cursor ? `?cursor=${encodeURIComponent(cursor)}` : '';
		const page: { data: N8nWorkflowLite[]; nextCursor: string | null } = await n8nFetch(apiKey, `/workflows${qs}`);
		all.push(...page.data);
		cursor = page.nextCursor;
		hasMore = cursor !== null;
	}

	return all;
}

export async function fetchRecentFailedExecutions(
	apiKey: string,
	sinceIso: string
): Promise<N8nExecutionLite[]> {
	const page = await n8nFetch<{ data: N8nExecutionLite[] }>(apiKey, '/executions?status=error&limit=100');
	return page.data.filter((e) => e.startedAt >= sinceIso);
}

export async function fetchLastExecutionPerWorkflow(
	apiKey: string,
	workflowId: string
): Promise<N8nExecutionLite | undefined> {
	const page = await n8nFetch<{ data: N8nExecutionLite[] }>(apiKey, `/executions?workflowId=${workflowId}&limit=1`);
	return page.data[0];
}
