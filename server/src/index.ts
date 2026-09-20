import cors from '@fastify/cors';
import Fastify, { type FastifyRequest } from 'fastify';
import type { PushSubscription } from 'web-push';
import { config } from './config.js';
import { computeHealthSummary, markExecutionNotified, startHealthPoller } from './healthPoll.js';
import { notifyFailure } from './push.js';
import { addSubscription, removeSubscription } from './subscriptions.js';

const app = Fastify({ logger: true });

await app.register(cors, {
	origin: config.allowedOrigin,
	methods: ['GET', 'POST', 'PUT', 'DELETE'],
	allowedHeaders: ['Content-Type', 'X-N8N-API-KEY']
});

class MissingApiKeyError extends Error {}

app.setErrorHandler((err, _req, reply) => {
	if (err instanceof MissingApiKeyError) {
		return reply.status(401).send({ error: 'Clé API n8n manquante (en-tête X-N8N-API-KEY).' });
	}
	app.log.error(err);
	return reply.status(500).send({ error: 'Erreur interne' });
});

function apiKeyFromRequest(req: FastifyRequest): string {
	const key = req.headers['x-n8n-api-key'];
	if (typeof key !== 'string' || !key) throw new MissingApiKeyError();
	return key;
}

let pollerApiKey: string | null = null;
let pollerHandle: NodeJS.Timeout | null = null;

function ensurePollerStarted(apiKey: string) {
	if (pollerHandle && pollerApiKey === apiKey) return;
	if (pollerHandle) clearInterval(pollerHandle);
	pollerApiKey = apiKey;
	pollerHandle = startHealthPoller(apiKey);
}

// --- Proxy transparent vers l'API REST n8n (ajoute le CORS que n8n n'émet pas) ---
app.route({
	method: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH'],
	url: '/api/n8n/*',
	handler: async (req, reply) => {
		const apiKey = apiKeyFromRequest(req);
		const subPath = req.url.replace(/^\/api\/n8n/, '');

		const upstream = await fetch(`${config.n8nBaseUrl}${subPath}`, {
			method: req.method,
			headers: { 'Content-Type': 'application/json', 'X-N8N-API-KEY': apiKey },
			body: ['GET', 'HEAD'].includes(req.method) ? undefined : JSON.stringify(req.body)
		});

		reply.status(upstream.status);
		reply.header('Content-Type', upstream.headers.get('Content-Type') ?? 'application/json');
		return reply.send(await upstream.text());
	}
});

// --- Proxy des webhooks de production n8n (ex: https://n8n.example.com/webhook/<path>) ---
app.post<{ Params: { path: string } }>('/hooks/:path', async (req, reply) => {
	const upstream = await fetch(`${config.n8nBaseUrl.replace('/api/v1', '')}/webhook/${req.params.path}`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify(req.body)
	});
	reply.status(upstream.status);
	return reply.send(await upstream.text());
});

// --- Web Push ---
app.get('/push/vapid-public-key', async () => ({ vapidPublicKey: config.vapidPublicKey }));

app.post<{ Body: PushSubscription }>('/push/subscribe', async (req, reply) => {
	const apiKey = apiKeyFromRequest(req);
	await addSubscription(req.body);
	ensurePollerStarted(apiKey);
	return reply.status(201).send({ ok: true });
});

app.post<{ Body: { endpoint: string } }>('/push/unsubscribe', async (req, reply) => {
	apiKeyFromRequest(req);
	await removeSubscription(req.body.endpoint);
	return reply.send({ ok: true });
});

// --- Santé globale, réutilisée par la PWA et par le poller de fallback ---
app.get('/health/summary', async (req) => {
	const apiKey = apiKeyFromRequest(req);
	ensurePollerStarted(apiKey);
	return computeHealthSummary(apiKey);
});

interface N8nErrorWebhookBody {
	workflow?: { id?: string; name?: string };
	execution?: { id?: string; error?: { message?: string } };
}

/**
 * Appelé par l'Error Workflow n8n (assigné comme "Error Workflow" par défaut
 * sur les workflows de prod) : voie instantanée de notification, en
 * complément du sondage de fallback. Non authentifié par clé API n8n (n8n
 * n'en envoie pas) : à restreindre à l'origine réseau du VPS via le reverse
 * proxy — voir docs/NOTIFICATIONS.md.
 */
app.post<{ Body: N8nErrorWebhookBody }>('/hooks/n8n-error', async (req, reply) => {
	const { workflow, execution } = req.body;
	const workflowId = workflow?.id ?? 'inconnu';
	const workflowName = workflow?.name ?? 'Workflow';

	if (execution?.id) markExecutionNotified(execution.id);

	await notifyFailure({
		title: `Échec : ${workflowName}`,
		body: execution?.error?.message?.slice(0, 150) ?? 'Voir le détail dans l’app.',
		workflowId
	});

	return reply.status(204).send();
});

app.listen({ port: config.port, host: '0.0.0.0' });
