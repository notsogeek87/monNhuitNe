import { unreachableBackendMessage } from './client';
import type { HealthSummary } from './types';

/**
 * Contrairement aux autres appels, la santé globale est calculée côté backend
 * (server/src/healthPoll.ts) plutôt que recalculée dans le navigateur : ça évite
 * de paginer toutes les executions depuis le téléphone à chaque ouverture, et ça
 * réutilise le même calcul que le poller de fallback pour les notifications.
 */
export async function getHealthSummary(backendBaseUrl: string, apiKey: string): Promise<HealthSummary> {
	let res: Response;
	try {
		res = await fetch(`${backendBaseUrl}/health/summary`, {
			headers: { 'X-N8N-API-KEY': apiKey }
		});
	} catch {
		throw new Error(unreachableBackendMessage(backendBaseUrl));
	}
	if (!res.ok) throw new Error(`Impossible de charger la santé globale (${res.status})`);
	return res.json();
}
