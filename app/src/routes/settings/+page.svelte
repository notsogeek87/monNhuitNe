<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { detectCapabilities, type NotificationCapability } from '../../lib/notifications/capabilities';
	import { subscribeToPush, unsubscribeFromPush, getCurrentPushSubscription } from '../../lib/notifications/push';
	import { clearCredentials, hasStoredCredentials, saveCredentials } from '../../lib/storage/secureStore';
	import { lock, unlockedCredentials } from '../../lib/stores/session';

	let backendBaseUrl = '';
	let n8nProxyBaseUrl = '';
	let apiKey = '';
	let pin = '';
	let pinConfirm = '';
	let hasExisting = false;
	let saving = false;
	let saveError = '';
	let saveOk = false;

	let capability: NotificationCapability | null = null;
	let pushEnabled = false;
	let pushBusy = false;
	let pushError = '';

	onMount(async () => {
		hasExisting = await hasStoredCredentials();
		capability = detectCapabilities();
		const sub = await getCurrentPushSubscription().catch(() => null);
		pushEnabled = sub !== null;

		const creds = $unlockedCredentials;
		if (creds) {
			backendBaseUrl = creds.backendBaseUrl;
			n8nProxyBaseUrl = creds.n8nProxyBaseUrl;
			apiKey = creds.apiKey;
		}
	});

	async function save(event: SubmitEvent) {
		event.preventDefault();
		saveError = '';
		saveOk = false;

		if (pin.length < 4) {
			saveError = 'Le PIN doit faire au moins 4 chiffres.';
			return;
		}
		if (pin !== pinConfirm) {
			saveError = 'Les deux PIN ne correspondent pas.';
			return;
		}

		saving = true;
		try {
			await saveCredentials({ backendBaseUrl, n8nProxyBaseUrl, apiKey }, pin);
			unlockedCredentials.set({ backendBaseUrl, n8nProxyBaseUrl, apiKey });
			saveOk = true;
			pin = '';
			pinConfirm = '';
			goto('/');
		} catch (err) {
			saveError = (err as Error).message;
		} finally {
			saving = false;
		}
	}

	async function resetDevice() {
		if (!confirm('Supprimer la configuration enregistrée sur cet appareil ?')) return;
		await clearCredentials();
		lock();
		hasExisting = false;
		backendBaseUrl = '';
		n8nProxyBaseUrl = '';
		apiKey = '';
	}

	async function togglePush() {
		const creds = $unlockedCredentials;
		if (!creds) return;
		pushBusy = true;
		pushError = '';
		try {
			if (pushEnabled) {
				await unsubscribeFromPush(creds.backendBaseUrl, creds.apiKey);
				pushEnabled = false;
			} else {
				await subscribeToPush(creds.backendBaseUrl, creds.apiKey);
				pushEnabled = true;
			}
		} catch (err) {
			pushError = (err as Error).message;
		} finally {
			pushBusy = false;
		}
	}
</script>

<h1>Réglages</h1>

<form on:submit={save} class="card">
	<h2>Connexion n8n</h2>
	<label>
		<span class="muted">URL du backend (proxy + push)</span>
		<input type="url" placeholder="https://pwa-api.example.com" bind:value={backendBaseUrl} required />
		<p class="hint">
			L'adresse où tourne le backend (dossier <code>server/</code>), pas celle
			de n8n. Si vous ne l'avez pas déployé vous-même, demandez cette adresse
			à la personne qui gère le serveur.
		</p>
	</label>
	<label>
		<span class="muted">URL du proxy API n8n</span>
		<input type="url" placeholder="https://pwa-api.example.com/api/n8n" bind:value={n8nProxyBaseUrl} required />
		<p class="hint">En général l'URL du backend ci-dessus, suivie de <code>/api/n8n</code>.</p>
	</label>
	<label>
		<span class="muted">Clé API n8n</span>
		<input type="password" autocomplete="off" bind:value={apiKey} required />
		<p class="hint">
			Dans n8n : votre avatar/nom (en bas à gauche) → <strong>Settings</strong>
			→ <strong>n8n API</strong> → bouton <strong>Create an API key</strong>.
			Copiez-la tout de suite, elle ne sera plus affichée en entier ensuite.
		</p>
	</label>
	<label>
		<span class="muted">PIN de déverrouillage (chiffre l'accès sur cet appareil)</span>
		<input type="password" inputmode="numeric" bind:value={pin} required minlength="4" />
		<p class="hint">
			Un code que vous inventez vous-même (4 chiffres minimum), propre à cet
			appareil. Il n'est enregistré nulle part : si vous l'oubliez, il n'y a
			pas de récupération possible, il faut tout ressaisir depuis zéro.
		</p>
	</label>
	<label>
		<span class="muted">Confirmer le PIN</span>
		<input type="password" inputmode="numeric" bind:value={pinConfirm} required minlength="4" />
	</label>

	{#if saveError}<p class="error-text">{saveError}</p>{/if}
	{#if saveOk}<p class="muted">Enregistré.</p>{/if}

	<button class="btn" type="submit" disabled={saving}>
		{hasExisting ? 'Mettre à jour' : 'Enregistrer'}
	</button>
</form>

{#if hasExisting}
	<div class="card">
		<h2>Notifications</h2>
		{#if capability}
			<p class="muted">
				{#if capability.isIos && !capability.isStandalone}
					Installez l'app sur l'écran d'accueil pour activer les notifications sur iPhone.
				{:else if capability.isIos && (capability.iosVersion ?? 0) < 16.4}
					iOS {capability.iosVersion} détecté — Web Push nécessite iOS 16.4 ou plus récent. L'app
					utilisera la vérification à l'ouverture à la place.
				{:else}
					Recevez une alerte immédiate quand un workflow échoue.
				{/if}
			</p>
		{/if}
		<button class="btn" disabled={!capability?.pushIsUsable || pushBusy} on:click={togglePush}>
			{pushEnabled ? 'Désactiver les notifications' : 'Activer les notifications'}
		</button>
		{#if pushError}<p class="error-text">{pushError}</p>{/if}
	</div>

	<div class="card">
		<h2>Appareil</h2>
		<button class="btn secondary" on:click={resetDevice}>Supprimer la configuration de cet appareil</button>
	</div>
{/if}

<style>
	h1 {
		font-size: 1.3rem;
		margin: 4px 0 16px;
	}
	h2 {
		font-size: 1rem;
		margin: 0 0 12px;
	}
	form {
		display: flex;
		flex-direction: column;
		gap: 12px;
		margin-bottom: 16px;
	}
	label {
		display: flex;
		flex-direction: column;
		gap: 4px;
	}
	.error-text {
		color: var(--error);
	}
	.hint {
		font-size: 0.8rem;
		color: var(--text-muted);
		margin: 2px 0 0;
		line-height: 1.35;
	}
	.hint code {
		font-size: 0.8rem;
	}
	.card + .card {
		margin-top: 16px;
	}
</style>
