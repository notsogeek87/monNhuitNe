<script lang="ts">
	import type { WorkflowWithLastRun } from '../api/types';
	import StatusBadge from './StatusBadge.svelte';

	export let workflow: WorkflowWithLastRun;

	function formatRelativeTime(iso: string | undefined): string {
		if (!iso) return 'jamais exécuté';
		const diffMs = Date.now() - new Date(iso).getTime();
		const minutes = Math.round(diffMs / 60000);
		if (minutes < 1) return "à l'instant";
		if (minutes < 60) return `il y a ${minutes} min`;
		const hours = Math.round(minutes / 60);
		if (hours < 24) return `il y a ${hours} h`;
		return `il y a ${Math.round(hours / 24)} j`;
	}
</script>

<a class="card workflow-card" href={`/workflows/${workflow.id}`}>
	<div class="row">
		<span class="name">{workflow.name}</span>
		<StatusBadge status={workflow.active ? 'success' : 'inactive'} />
	</div>
	<div class="row">
		{#if workflow.lastExecution}
			<StatusBadge status={workflow.lastExecution.status} />
			<span class="muted">{formatRelativeTime(workflow.lastExecution.startedAt)}</span>
		{:else}
			<span class="muted">jamais exécuté</span>
		{/if}
	</div>
</a>

<style>
	.workflow-card {
		display: block;
		margin-bottom: 10px;
		color: var(--text);
	}
	.row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 8px;
		margin-bottom: 6px;
	}
	.row:last-child {
		margin-bottom: 0;
	}
	.name {
		font-weight: 600;
	}
</style>
