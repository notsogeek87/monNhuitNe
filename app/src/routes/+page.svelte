<script lang="ts">
	import { onDestroy, onMount } from 'svelte';
	import InstallBanner from '../lib/components/InstallBanner.svelte';
	import WorkflowCard from '../lib/components/WorkflowCard.svelte';
	import WorkflowToolbar from '../lib/components/WorkflowToolbar.svelte';
	import { pollOnForeground, registerForegroundPolling } from '../lib/notifications/polling';
	import { backendBaseUrl, n8nConfig, unlockedCredentials } from '../lib/stores/session';
	import { resetWorkflowFilters, workflowFilters } from '../lib/stores/workflowFilters';
	import { workflowsStore } from '../lib/stores/workflows';
	import { filterAndSortWorkflows } from '../lib/workflows/filtering';

	let newFailuresBanner = false;
	let unregisterPolling: (() => void) | undefined;

	$: filteredWorkflows = filterAndSortWorkflows($workflowsStore.items, $workflowFilters);

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
{:else if $workflowsStore.items.length === 0}
	<p class="muted">Aucun workflow trouvé.</p>
{:else}
	<WorkflowToolbar items={$workflowsStore.items} />

	{#if filteredWorkflows.length === 0}
		<div class="card empty-filtered">
			<p class="muted" style="margin-top:0">Aucun workflow ne correspond à ces filtres.</p>
			<button type="button" class="btn secondary" on:click={resetWorkflowFilters}>Réinitialiser les filtres</button>
		</div>
	{:else}
		<p class="muted result-count">
			{filteredWorkflows.length} workflow{filteredWorkflows.length > 1 ? 's' : ''}{filteredWorkflows.length !==
			$workflowsStore.items.length
				? ` sur ${$workflowsStore.items.length}`
				: ''}
		</p>
		{#each filteredWorkflows as workflow (workflow.id)}
			<WorkflowCard {workflow} />
		{/each}
	{/if}
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
	.result-count {
		margin: 0 2px 10px;
	}
	.empty-filtered {
		display: flex;
		flex-direction: column;
		gap: 10px;
	}
</style>
