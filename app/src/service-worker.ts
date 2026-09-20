/// <reference lib="webworker" />
import { cleanupOutdatedCaches, createHandlerBoundToURL, precacheAndRoute } from 'workbox-precaching';
import { NavigationRoute, registerRoute } from 'workbox-routing';
import { NetworkFirst } from 'workbox-strategies';

declare let self: ServiceWorkerGlobalScope;

// Injecté par vite-plugin-pwa (injectManifest) à la compilation.
precacheAndRoute(self.__WB_MANIFEST);
cleanupOutdatedCaches();

// Coquille app (HTML/JS/CSS) précachée, navigation offline vers la dernière version connue.
registerRoute(new NavigationRoute(createHandlerBoundToURL('/index.html')));

// Les appels au proxy backend (/api/n8n/*, /health/*) restent "network first" :
// on veut toujours des données fraîches quand le réseau est là, mais on tolère
// une réponse en cache si offline (liste des workflows vue en dernier, par ex).
registerRoute(
	({ url }) => url.pathname.startsWith('/api/n8n') || url.pathname.startsWith('/health'),
	new NetworkFirst({ cacheName: 'n8n-api-cache', networkTimeoutSeconds: 5 })
);

self.addEventListener('push', (event: PushEvent) => {
	const payload = event.data?.json() ?? {};
	const title = payload.title ?? 'Échec de workflow';
	const body = payload.body ?? 'Un workflow a échoué. Ouvrez l’app pour le détail.';
	const workflowId = payload.workflowId as string | undefined;

	event.waitUntil(
		self.registration.showNotification(title, {
			body,
			icon: '/icons/icon-192.png',
			badge: '/icons/icon-192.png',
			tag: workflowId ?? 'monnhuitne-failure',
			data: { workflowId }
		})
	);
});

self.addEventListener('notificationclick', (event: NotificationEvent) => {
	event.notification.close();
	const workflowId = event.notification.data?.workflowId as string | undefined;
	const targetUrl = workflowId ? `/workflows/${workflowId}` : '/';

	event.waitUntil(
		self.clients.matchAll({ type: 'window' }).then((clients) => {
			const existing = clients.find((c) => 'focus' in c);
			if (existing) {
				existing.navigate(targetUrl);
				return existing.focus();
			}
			return self.clients.openWindow(targetUrl);
		})
	);
});

self.addEventListener('message', (event) => {
	if (event.data?.type === 'SKIP_WAITING') self.skipWaiting();
});
