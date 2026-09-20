export interface NotificationCapability {
	isStandalone: boolean;
	isIos: boolean;
	iosVersion: number | null;
	supportsPush: boolean;
	/** true si le Web Push a une chance réelle de fonctionner sur cet appareil maintenant. */
	pushIsUsable: boolean;
	/** true si l'app tourne dans un onglet Safari classique et pourrait fonctionner si installée. */
	shouldPromptInstall: boolean;
}

function detectIosVersion(): number | null {
	const match = navigator.userAgent.match(/OS (\d+)_(\d+)/);
	if (!match) return null;
	return parseFloat(`${match[1]}.${match[2]}`);
}

function detectIsIos(): boolean {
	return /iP(hone|od|ad)/.test(navigator.userAgent) || (navigator.userAgent.includes('Macintosh') && navigator.maxTouchPoints > 1);
}

export function detectCapabilities(): NotificationCapability {
	const isStandalone =
		window.matchMedia('(display-mode: standalone)').matches ||
		// @ts-expect-error API non standard mais présente sur iOS Safari
		window.navigator.standalone === true;

	const isIos = detectIsIos();
	const iosVersion = isIos ? detectIosVersion() : null;
	const supportsPush = 'serviceWorker' in navigator && 'PushManager' in window && 'Notification' in window;

	// Sur iOS : push utilisable seulement si iOS >= 16.4 ET app installée en standalone.
	// Sur les autres plateformes (Android/desktop) : le support natif suffit.
	const pushIsUsable = isIos ? supportsPush && isStandalone && (iosVersion ?? 0) >= 16.4 : supportsPush;

	const shouldPromptInstall = isIos && !isStandalone;

	return { isStandalone, isIos, iosVersion, supportsPush, pushIsUsable, shouldPromptInstall };
}
