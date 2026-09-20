import { beforeEach, describe, expect, it, vi } from 'vitest';
import { detectTriggerInputs, listWorkflows, triggerWorkflow } from './workflows';
import type { N8nWorkflow } from './types';

const config = { proxyBaseUrl: 'https://backend.test/api/n8n', apiKey: 'test-key' };

beforeEach(() => {
	vi.restoreAllMocks();
});

describe('listWorkflows', () => {
	it('suit la pagination via nextCursor jusqu\'à épuisement', async () => {
		const fetchMock = vi
			.fn()
			.mockResolvedValueOnce(
				jsonResponse({ data: [{ id: '1', name: 'A' }], nextCursor: 'page2' })
			)
			.mockResolvedValueOnce(jsonResponse({ data: [{ id: '2', name: 'B' }], nextCursor: null }));
		vi.stubGlobal('fetch', fetchMock);

		const result = await listWorkflows(config);

		expect(result.map((w) => w.id)).toEqual(['1', '2']);
		expect(fetchMock).toHaveBeenCalledTimes(2);
		expect(fetchMock.mock.calls[1][0]).toContain('cursor=page2');
	});
});

describe('triggerWorkflow', () => {
	it('appelle /workflows/:id/execute en mode execute', async () => {
		const fetchMock = vi.fn().mockResolvedValueOnce(jsonResponse({ executionId: 'exec-1' }));
		vi.stubGlobal('fetch', fetchMock);

		const result = await triggerWorkflow(config, 'wf-1', { mode: 'execute', payload: { foo: 'bar' } });

		expect(result.executionId).toBe('exec-1');
		expect(fetchMock).toHaveBeenCalledWith(
			'https://backend.test/api/n8n/workflows/wf-1/execute',
			expect.objectContaining({ method: 'POST' })
		);
	});

	it('appelle le webhook proxifié en mode webhook', async () => {
		const fetchMock = vi.fn().mockResolvedValueOnce(new Response(null, { status: 200 }));
		vi.stubGlobal('fetch', fetchMock);

		await triggerWorkflow(config, 'wf-1', { mode: 'webhook', webhookPath: 'my-hook', payload: {} });

		expect(fetchMock).toHaveBeenCalledWith(
			'https://backend.test/hooks/my-hook',
			expect.objectContaining({ method: 'POST' })
		);
	});
});

describe('detectTriggerInputs', () => {
	it('extrait les champs déclarés sur un Execute Workflow Trigger', () => {
		const workflow: N8nWorkflow = {
			id: 'wf-1',
			name: 'Test',
			active: true,
			createdAt: '',
			updatedAt: '',
			nodes: [
				{
					id: 'n1',
					name: 'Trigger',
					type: 'n8n-nodes-base.executeWorkflowTrigger',
					parameters: { workflowInputs: { values: [{ name: 'email', type: 'string' }] } }
				}
			]
		};

		expect(detectTriggerInputs(workflow)).toEqual([
			{ key: 'email', label: 'email', type: 'string', required: true }
		]);
	});

	it('renvoie un tableau vide si aucun trigger reconnu', () => {
		const workflow: N8nWorkflow = { id: 'wf-1', name: 'Test', active: true, createdAt: '', updatedAt: '' };
		expect(detectTriggerInputs(workflow)).toEqual([]);
	});
});

function jsonResponse(body: unknown): Response {
	return new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } });
}
