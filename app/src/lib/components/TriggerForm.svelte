<script lang="ts">
	import type { TriggerInputField } from '../api/types';

	export let fields: TriggerInputField[];
	export let submitting = false;
	export let onSubmit: (payload: Record<string, unknown>) => void;

	let values: Record<string, string> = {};

	function submit(event: SubmitEvent) {
		event.preventDefault();
		const payload: Record<string, unknown> = {};
		for (const field of fields) {
			const raw = values[field.key] ?? '';
			payload[field.key] = field.type === 'number' ? Number(raw) : field.type === 'boolean' ? raw === 'true' : raw;
		}
		onSubmit(payload);
	}
</script>

<form on:submit={submit} class="card">
	{#if fields.length === 0}
		<p class="muted">Ce workflow ne semble attendre aucun paramètre d'entrée.</p>
	{/if}

	{#each fields as field}
		<label>
			<span class="muted">{field.label}{field.required ? ' *' : ''}</span>
			{#if field.type === 'boolean'}
				<select bind:value={values[field.key]}>
					<option value="false">Non</option>
					<option value="true">Oui</option>
				</select>
			{:else if field.type === 'number'}
				<input type="number" required={field.required} bind:value={values[field.key]} />
			{:else}
				<input type="text" required={field.required} bind:value={values[field.key]} />
			{/if}
		</label>
	{/each}

	<button class="btn" type="submit" disabled={submitting}>
		{submitting ? 'Déclenchement…' : 'Déclencher le workflow'}
	</button>
</form>

<style>
	form {
		display: flex;
		flex-direction: column;
		gap: 12px;
	}
	label {
		display: flex;
		flex-direction: column;
		gap: 4px;
	}
</style>
