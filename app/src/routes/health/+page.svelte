<script lang="ts">
	import { onMount } from 'svelte';
	import HealthSummary from '../../lib/components/HealthSummary.svelte';
	import { getHealthSummary } from '../../lib/api/health';
	import type { HealthSummary as HealthSummaryType } from '../../lib/api/types';
	import { unlockedCredentials } from '../../lib/stores/session';

	let summary: HealthSummaryType | null = null;
	let error: string | null = null;

	onMount(async () => {
		const creds = $unlockedCredentials;
		if (!creds) return;
		try {
			summary = await getHealthSummary(creds.backendBaseUrl, creds.apiKey);
		} catch (err) {
			error = (err as Error).message;
		}
	});
</script>

<h1>Santé globale</h1>

{#if error}
	<p class="error-text">{error}</p>
	<p class="muted"><a href="/settings">Vérifier les réglages de connexion</a></p>
{:else if summary}
	<HealthSummary {summary} />
{:else}
	<p class="muted">Chargement…</p>
{/if}

<style>
	h1 {
		font-size: 1.3rem;
		margin: 4px 0 16px;
	}
	.error-text {
		color: var(--error);
	}
</style>
