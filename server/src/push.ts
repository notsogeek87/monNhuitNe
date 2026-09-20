import webpush from 'web-push';
import { config } from './config.js';
import { listSubscriptions, removeSubscriptionsByEndpoints } from './subscriptions.js';

const vapidConfigured = Boolean(config.vapidPublicKey && config.vapidPrivateKey);
if (vapidConfigured) {
	webpush.setVapidDetails(config.vapidSubject, config.vapidPublicKey, config.vapidPrivateKey);
}

export interface FailureNotification {
	title: string;
	body: string;
	workflowId: string;
}

/**
 * Envoie à tous les appareils abonnés. Une souscription expirée (410/404,
 * l'utilisateur a désinstallé l'app ou révoqué la permission) est nettoyée
 * automatiquement plutôt que retentée indéfiniment.
 *
 * No-op tant que les clés VAPID ne sont pas configurées (cf `.env.example`,
 * `npx web-push generate-vapid-keys`) : évite de planter le process au
 * démarrage avant la première mise en service.
 */
export async function notifyFailure(notification: FailureNotification): Promise<void> {
	if (!vapidConfigured) {
		console.warn('[push] VAPID non configuré, notification ignorée:', notification.title);
		return;
	}

	const subscriptions = await listSubscriptions();
	const expired: string[] = [];

	await Promise.all(
		subscriptions.map(async (subscription) => {
			try {
				await webpush.sendNotification(subscription, JSON.stringify(notification));
			} catch (err) {
				const statusCode = (err as { statusCode?: number }).statusCode;
				if (statusCode === 404 || statusCode === 410) {
					expired.push(subscription.endpoint);
				}
			}
		})
	);

	await removeSubscriptionsByEndpoints(expired);
}
