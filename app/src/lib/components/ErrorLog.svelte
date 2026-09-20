<script lang="ts">
	import type { ExecutionErrorSummary } from '../api/types';

	export let error: ExecutionErrorSummary;
</script>

<div class="card error-log">
	<p class="node">Échec dans le node <strong>{error.nodeName}</strong></p>
	<p class="message">{error.message}</p>
	<p class="muted">{new Date(error.timestamp).toLocaleString('fr-FR')}</p>
</div>

<style>
	.error-log {
		border-color: var(--error);
	}
	.node {
		margin: 0 0 6px;
		font-size: 0.85rem;
		color: var(--text-muted);
	}
	.message {
		margin: 0 0 8px;
		color: var(--error);
		font-weight: 500;
		white-space: pre-wrap;
	}
</style>
