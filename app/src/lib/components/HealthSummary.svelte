<script lang="ts">
	import type { HealthSummary } from '../api/types';

	export let summary: HealthSummary;
</script>

<div class="card">
	<p class="metric" class:bad={summary.failuresLast24h > 0}>
		{summary.failuresLast24h}
	</p>
	<p class="muted">échec(s) sur les 24 dernières heures</p>
</div>

{#if summary.staleWorkflows.length > 0}
	<div class="card">
		<p class="muted" style="margin-top:0">Workflows inactifs depuis longtemps</p>
		<ul>
			{#each summary.staleWorkflows as wf}
				<li>
					<a href={`/workflows/${wf.id}`}>{wf.name}</a>
					<span class="muted"> — {wf.daysSinceLastRun} j sans exécution</span>
				</li>
			{/each}
		</ul>
	</div>
{/if}

<style>
	.metric {
		font-size: 2.5rem;
		font-weight: 700;
		margin: 0;
		color: var(--success);
	}
	.metric.bad {
		color: var(--error);
	}
	ul {
		margin: 0;
		padding-left: 18px;
	}
	li {
		margin-bottom: 6px;
	}
</style>
