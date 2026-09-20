import { beforeEach, describe, expect, it, vi } from 'vitest';
import { findWebhookPath, listWorkflows, triggerWorkflow } from './workflows';
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
	it('appelle le webhook proxifié', async () => {
		const fetchMock = vi.fn().mockResolvedValueOnce(new Response(null, { status: 200 }));
		vi.stubGlobal('fetch', fetchMock);

		await triggerWorkflow(config, 'my-hook', {});

		expect(fetchMock).toHaveBeenCalledWith(
			'https://backend.test/hooks/my-hook',
			expect.objectContaining({ method: 'POST' })
		);
	});
});

describe('findWebhookPath', () => {
	it('trouve le chemin déclaré sur un node Webhook', () => {
		const workflow: N8nWorkflow = {
			id: 'wf-1',
			name: 'Test',
			active: true,
			createdAt: '',
			updatedAt: '',
			nodes: [
				{
					id: 'n1',
					name: 'Webhook',
					type: 'n8n-nodes-base.webhook',
					parameters: { path: 'my-hook' }
				}
			]
		};

		expect(findWebhookPath(workflow)).toBe('my-hook');
	});

	it("renvoie null si le workflow n'a pas de node Webhook", () => {
		const workflow: N8nWorkflow = { id: 'wf-1', name: 'Test', active: true, createdAt: '', updatedAt: '' };
		expect(findWebhookPath(workflow)).toBeNull();
	});
});

function jsonResponse(body: unknown): Response {
	return new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } });
}
