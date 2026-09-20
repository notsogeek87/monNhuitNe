import { getHealthSummary } from '../api/health';

const LAST_SEEN_FAILURES_KEY = 'monnhuitne:lastSeenFailureCount';

export interface PollResult {
	hasNewFailures: boolean;
	failuresLast24h: number;
}

/**
 * Fallback pour les appareils où le Web Push n'est pas utilisable (iOS < 16.4,
 * PWA non installée, permission refusée) : appelé sur `visibilitychange`/`focus`
 * plutôt que sur un minuteur, pour ne pas dépenser de batterie en arrière-plan
 * (un service worker ne peut pas se réveiller seul sans push sur iOS).
 */
export async function pollOnForeground(backendBaseUrl: string, apiKey: string): Promise<PollResult> {
	const summary = await getHealthSummary(backendBaseUrl, apiKey);
	const lastSeen = Number(localStorage.getItem(LAST_SEEN_FAILURES_KEY) ?? '0');

	const hasNewFailures = summary.failuresLast24h > lastSeen;
	localStorage.setItem(LAST_SEEN_FAILURES_KEY, String(summary.failuresLast24h));

	return { hasNewFailures, failuresLast24h: summary.failuresLast24h };
}

export function registerForegroundPolling(callback: () => void): () => void {
	const handler = () => {
		if (document.visibilityState === 'visible') callback();
	};

	document.addEventListener('visibilitychange', handler);
	window.addEventListener('focus', handler);

	// Premier check immédiat au montage.
	handler();

	return () => {
		document.removeEventListener('visibilitychange', handler);
		window.removeEventListener('focus', handler);
	};
}
