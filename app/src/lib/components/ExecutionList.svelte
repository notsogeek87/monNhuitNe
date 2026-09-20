<script lang="ts">
	import type { N8nExecution } from '../api/types';
	import StatusBadge from './StatusBadge.svelte';

	export let executions: N8nExecution[];
	export let onSelect: (execution: N8nExecution) => void = () => {};

	function formatDateTime(iso: string): string {
		return new Date(iso).toLocaleString('fr-FR', {
			day: '2-digit',
			month: '2-digit',
			hour: '2-digit',
			minute: '2-digit'
		});
	}

	function duration(execution: N8nExecution): string {
		if (!execution.stoppedAt) return '';
		const ms = new Date(execution.stoppedAt).getTime() - new Date(execution.startedAt).getTime();
		return `${(ms / 1000).toFixed(1)}s`;
	}
</script>

<ul>
	{#each executions as execution}
		<li>
			<button class="card row" on:click={() => onSelect(execution)}>
				<div class="left">
					<StatusBadge status={execution.status} />
					<span class="muted">{formatDateTime(execution.startedAt)}</span>
				</div>
				<span class="muted">{duration(execution)}</span>
			</button>
		</li>
	{:else}
		<p class="muted">Aucune exécution récente.</p>
	{/each}
</ul>

<style>
	ul {
		list-style: none;
		margin: 0;
		padding: 0;
	}
	li {
		margin-bottom: 8px;
	}
	.row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		width: 100%;
		background: var(--bg-card);
		text-align: left;
		border: 1px solid var(--border);
		cursor: pointer;
	}
	.left {
		display: flex;
		align-items: center;
		gap: 10px;
	}
</style>
