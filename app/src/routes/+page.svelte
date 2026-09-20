<script lang="ts">
	import { onDestroy, onMount } from 'svelte';
	import InstallBanner from '../lib/components/InstallBanner.svelte';
	import WorkflowCard from '../lib/components/WorkflowCard.svelte';
	import { pollOnForeground, registerForegroundPolling } from '../lib/notifications/polling';
	import { backendBaseUrl, n8nConfig, unlockedCredentials } from '../lib/stores/session';
	import { workflowsStore } from '../lib/stores/workflows';

	let newFailuresBanner = false;
	let unregisterPolling: (() => void) | undefined;

	onMount(() => {
		const unsubscribe = n8nConfig.subscribe((config) => {
			if (config) workflowsStore.refresh(config);
		});

		unregisterPolling = registerForegroundPolling(async () => {
			const creds = $unlockedCredentials;
			if (!creds) return;
			try {
				const result = await pollOnForeground(creds.backendBaseUrl, creds.apiKey);
				newFailuresBanner = result.hasNewFailures;
				if ($n8nConfig) workflowsStore.refresh($n8nConfig);
			} catch {
				// Backend indisponible : on affiche silencieusement les dernières données connues.
			}
		});

		return unsubscribe;
	});

	onDestroy(() => unregisterPolling?.());
</script>

<InstallBanner />

{#if newFailuresBanner}
	<div class="card banner">
		<a href="/health">Nouveaux échecs détectés depuis votre dernière visite — voir la santé globale</a>
	</div>
{/if}

<h1>Workflows</h1>

{#if $workflowsStore.loading}
	<p class="muted">Chargement…</p>
{:else if $workflowsStore.error}
	<p class="error-text">{$workflowsStore.error}</p>
	<p class="muted"><a href="/settings">Vérifier les réglages de connexion</a></p>
{:else}
	{#each $workflowsStore.items as workflow}
		<WorkflowCard {workflow} />
	{:else}
		<p class="muted">Aucun workflow trouvé.</p>
	{/each}
{/if}

<style>
	h1 {
		font-size: 1.3rem;
		margin: 4px 0 16px;
	}
	.banner {
		margin-bottom: 12px;
		border-color: var(--warning);
	}
	.error-text {
		color: var(--error);
	}
</style>
