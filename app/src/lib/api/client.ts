// Client HTTP bas niveau, sans dépendance UI : reçoit sa config en paramètre,
// ne lit jamais un store Svelte directement. Testable en isolation avec msw.

export interface N8nClientConfig {
	/** URL du proxy backend, ex: https://pwa-api.lielu.eu/api/n8n (pas l'API n8n directement, cf CORS). */
	proxyBaseUrl: string;
	apiKey: string;
}

export class N8nApiError extends Error {
	constructor(
		public status: number,
		message: string
	) {
		super(message);
		this.name = 'N8nApiError';
	}
}

/** Message explicite à la place du générique "Failed to fetch" du navigateur quand la requête n'a pas pu partir. */
export function unreachableBackendMessage(url: string): string {
	return (
		`Impossible de joindre ${url}. Vérifiez dans Réglages : l'URL est correcte ` +
		`(pas de faute de frappe), le serveur backend est démarré et accessible depuis cet ` +
		`appareil, et votre connexion internet fonctionne.`
	);
}

export async function n8nRequest<T>(
	config: N8nClientConfig,
	path: string,
	init: RequestInit = {}
): Promise<T> {
	let res: Response;
	try {
		res = await fetch(`${config.proxyBaseUrl}${path}`, {
			...init,
			headers: {
				'Content-Type': 'application/json',
				'X-N8N-API-KEY': config.apiKey,
				...init.headers
			}
		});
	} catch {
		throw new Error(unreachableBackendMessage(config.proxyBaseUrl));
	}

	if (!res.ok) {
		const body = await res.text().catch(() => '');
		throw new N8nApiError(res.status, `n8n API ${res.status} sur ${path}: ${body.slice(0, 200)}`);
	}

	if (res.status === 204) {
		return undefined as T;
	}

	return (await res.json()) as T;
}
