<script lang="ts">
	import { onMount } from 'svelte';
	import { page } from '$app/stores';
	import ErrorLog from '../../../lib/components/ErrorLog.svelte';
	import ExecutionList from '../../../lib/components/ExecutionList.svelte';
	import StatusBadge from '../../../lib/components/StatusBadge.svelte';
	import TriggerForm from '../../../lib/components/TriggerForm.svelte';
	import { detectTriggerInputs, getWorkflow, triggerWorkflow } from '../../../lib/api/workflows';
	import { getExecutionErrorSummary, listExecutions } from '../../../lib/api/executions';
	import type { ExecutionErrorSummary, N8nExecution, N8nWorkflow, TriggerInputField } from '../../../lib/api/types';
	import { n8nConfig } from '../../../lib/stores/session';

	$: workflowId = $page.params.id ?? '';

	let workflow: N8nWorkflow | null = null;
	let executions: N8nExecution[] = [];
	let triggerFields: TriggerInputField[] = [];
	let selectedError: ExecutionErrorSummary | undefined;
	let loading = true;
	let error: string | null = null;
	let triggering = false;
	let triggerFeedback: string | null = null;

	async function load() {
		const config = $n8nConfig;
		if (!config) return;
		loading = true;
		error = null;
		try {
			workflow = await getWorkflow(config, workflowId);
			executions = await listExecutions(config, { workflowId, limit: 20 });
			triggerFields = detectTriggerInputs(workflow);
		} catch (err) {
			error = (err as Error).message;
		} finally {
			loading = false;
		}
	}

	onMount(load);

	async function selectExecution(execution: N8nExecution) {
		const config = $n8nConfig;
		if (!config || execution.status !== 'error') {
			selectedError = undefined;
			return;
		}
		selectedError = await getExecutionErrorSummary(config, execution.id);
	}

	function findWebhookPath(): string | null {
		const webhookNode = workflow?.nodes?.find((n) => n.type.includes('webhook'));
		return (webhookNode?.parameters?.path as string) ?? null;
	}

	async function handleTrigger(payload: Record<string, unknown>) {
		const config = $n8nConfig;
		if (!config || !workflow) return;
		triggering = true;
		triggerFeedback = null;
		try {
			const webhookPath = findWebhookPath();
			if (webhookPath) {
				await triggerWorkflow(config, workflow.id, { mode: 'webhook', webhookPath, payload });
			} else {
				await triggerWorkflow(config, workflow.id, { mode: 'execute', payload });
			}
			triggerFeedback = 'Déclenché. Rafraîchissement des exécutions…';
			await load();
		} catch (err) {
			triggerFeedback = `Échec du déclenchement : ${(err as Error).message}`;
		} finally {
			triggering = false;
		}
	}
</script>

{#if loading}
	<p class="muted">Chargement…</p>
{:else if error}
	<p class="error-text">{error}</p>
{:else if workflow}
	<a class="back muted" href="/">← Workflows</a>
	<h1>{workflow.name}</h1>
	<div class="status-row">
		<StatusBadge status={workflow.active ? 'success' : 'inactive'} />
	</div>

	<section>
		<h2>Déclencher</h2>
		<TriggerForm fields={triggerFields} submitting={triggering} onSubmit={handleTrigger} />
		{#if triggerFeedback}<p class="muted">{triggerFeedback}</p>{/if}
	</section>

	<section>
		<h2>Exécutions récentes</h2>
		<ExecutionList {executions} onSelect={selectExecution} />
		{#if selectedError}
			<ErrorLog error={selectedError} />
		{/if}
	</section>
{/if}

<style>
	.back {
		display: inline-block;
		margin-bottom: 8px;
	}
	h1 {
		font-size: 1.3rem;
		margin: 0 0 8px;
	}
	.status-row {
		margin-bottom: 20px;
	}
	h2 {
		font-size: 1rem;
		margin: 24px 0 10px;
	}
	.error-text {
		color: var(--error);
	}
</style>
