function urlBase64ToUint8Array(base64String: string): Uint8Array {
	const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
	const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
	const rawData = atob(base64);
	return Uint8Array.from([...rawData].map((c) => c.charCodeAt(0)));
}

export async function subscribeToPush(backendBaseUrl: string, apiKey: string): Promise<PushSubscription> {
	const permission = await Notification.requestPermission();
	if (permission !== 'granted') {
		throw new Error('Permission de notification refusée.');
	}

	const registration = await navigator.serviceWorker.ready;

	const { vapidPublicKey } = await fetch(`${backendBaseUrl}/push/vapid-public-key`).then((r) => r.json());

	const subscription = await registration.pushManager.subscribe({
		userVisibleOnly: true,
		applicationServerKey: urlBase64ToUint8Array(vapidPublicKey) as BufferSource
	});

	await fetch(`${backendBaseUrl}/push/subscribe`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json', 'X-N8N-API-KEY': apiKey },
		body: JSON.stringify(subscription.toJSON())
	});

	return subscription;
}

export async function unsubscribeFromPush(backendBaseUrl: string, apiKey: string): Promise<void> {
	const registration = await navigator.serviceWorker.ready;
	const subscription = await registration.pushManager.getSubscription();
	if (!subscription) return;

	await fetch(`${backendBaseUrl}/push/unsubscribe`, {
		method: 'POST',
		headers: { 'Content-Type': 'application/json', 'X-N8N-API-KEY': apiKey },
		body: JSON.stringify({ endpoint: subscription.endpoint })
	});

	await subscription.unsubscribe();
}

export async function getCurrentPushSubscription(): Promise<PushSubscription | null> {
	if (!('serviceWorker' in navigator)) return null;
	const registration = await navigator.serviceWorker.ready;
	return registration.pushManager.getSubscription();
}
