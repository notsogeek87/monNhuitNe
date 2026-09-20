import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { dirname } from 'node:path';
import type { PushSubscription } from 'web-push';
import { config } from './config.js';

// Stockage JSON minimal plutôt qu'une vraie base : le volume attendu (quelques
// appareils personnels) ne justifie pas une dépendance supplémentaire.
let cache: PushSubscription[] | null = null;

async function load(): Promise<PushSubscription[]> {
	if (cache) return cache;
	try {
		const raw = await readFile(config.subscriptionsFile, 'utf-8');
		cache = JSON.parse(raw);
	} catch {
		cache = [];
	}
	return cache!;
}

async function persist(subs: PushSubscription[]): Promise<void> {
	cache = subs;
	await mkdir(dirname(config.subscriptionsFile), { recursive: true });
	await writeFile(config.subscriptionsFile, JSON.stringify(subs, null, 2));
}

export async function addSubscription(subscription: PushSubscription): Promise<void> {
	const subs = await load();
	if (subs.some((s) => s.endpoint === subscription.endpoint)) return;
	await persist([...subs, subscription]);
}

export async function removeSubscription(endpoint: string): Promise<void> {
	const subs = await load();
	await persist(subs.filter((s) => s.endpoint !== endpoint));
}

export async function removeSubscriptionsByEndpoints(endpoints: string[]): Promise<void> {
	if (endpoints.length === 0) return;
	const subs = await load();
	await persist(subs.filter((s) => !endpoints.includes(s.endpoint)));
}

export async function listSubscriptions(): Promise<PushSubscription[]> {
	return load();
}
