function required(name: string, fallback?: string): string {
	const value = process.env[name] ?? fallback;
	if (value === undefined) throw new Error(`Variable d'environnement manquante: ${name}`);
	return value;
}

export const config = {
	n8nBaseUrl: required('N8N_BASE_URL'),
	allowedOrigin: required('ALLOWED_ORIGIN'),
	port: Number(process.env.PORT ?? 8787),
	vapidPublicKey: process.env.VAPID_PUBLIC_KEY ?? '',
	vapidPrivateKey: process.env.VAPID_PRIVATE_KEY ?? '',
	vapidSubject: required('VAPID_SUBJECT', 'mailto:admin@example.com'),
	healthPollIntervalMinutes: Number(process.env.HEALTH_POLL_INTERVAL_MINUTES ?? 10),
	staleWorkflowDays: Number(process.env.STALE_WORKFLOW_DAYS ?? 7),
	subscriptionsFile: process.env.SUBSCRIPTIONS_FILE ?? './data/subscriptions.json'
};
