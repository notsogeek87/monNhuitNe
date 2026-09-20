<script lang="ts">
	import type { ExecutionStatus } from '../api/types';

	export let status: ExecutionStatus | 'inactive' | 'unknown';

	const labels: Record<string, string> = {
		success: 'OK',
		error: 'Échec',
		running: 'En cours',
		waiting: 'En attente',
		canceled: 'Annulé',
		inactive: 'Inactif',
		unknown: 'Aucune donnée'
	};

	const colors: Record<string, string> = {
		success: 'var(--success)',
		error: 'var(--error)',
		running: 'var(--accent)',
		waiting: 'var(--warning)',
		canceled: 'var(--text-muted)',
		inactive: 'var(--text-muted)',
		unknown: 'var(--text-muted)'
	};
</script>

<span class="badge" style:--dot={colors[status]}>
	<span class="dot" />
	{labels[status]}
</span>

<style>
	.badge {
		display: inline-flex;
		align-items: center;
		gap: 6px;
		font-size: 0.8rem;
		font-weight: 600;
	}
	.dot {
		width: 8px;
		height: 8px;
		border-radius: 50%;
		background: var(--dot);
	}
</style>
