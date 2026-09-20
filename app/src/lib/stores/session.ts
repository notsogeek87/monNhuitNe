import { derived, writable } from 'svelte/store';
import type { N8nClientConfig } from '../api/client';
import type { StoredCredentials } from '../storage/secureStore';

/**
 * État déverrouillé de la session, en mémoire uniquement (jamais persisté en clair).
 * `null` = verrouillé / pas encore déverrouillé depuis le dernier chargement de page.
 */
export const unlockedCredentials = writable<StoredCredentials | null>(null);

export const isUnlocked = derived(unlockedCredentials, ($c) => $c !== null);

export const n8nConfig = derived<typeof unlockedCredentials, N8nClientConfig | null>(
	unlockedCredentials,
	($c) => ($c ? { proxyBaseUrl: $c.n8nProxyBaseUrl, apiKey: $c.apiKey } : null)
);

export const backendBaseUrl = derived(unlockedCredentials, ($c) => $c?.backendBaseUrl ?? null);

export function lock(): void {
	unlockedCredentials.set(null);
}
