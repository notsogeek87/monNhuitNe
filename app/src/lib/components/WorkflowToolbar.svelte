<script lang="ts">
	import type { WorkflowWithLastRun } from '../api/types';
	import { workflowFilters } from '../stores/workflowFilters';
	import { collectTagNames, computeStatusCounts, type SortBy, type StatusFilter } from '../workflows/filtering';

	export let items: WorkflowWithLastRun[];

	$: counts = computeStatusCounts(items);
	$: tagNames = collectTagNames(items);

	const statusOptions: { value: StatusFilter; label: string }[] = [
		{ value: 'all', label: 'Tous' },
		{ value: 'error', label: 'En échec' },
		{ value: 'active', label: 'Actifs' },
		{ value: 'inactive', label: 'Inactifs' },
		{ value: 'never', label: 'Jamais exécuté' }
	];

	const sortOptions: { value: SortBy; label: string }[] = [
		{ value: 'priority', label: 'Priorité (problèmes d’abord)' },
		{ value: 'name', label: 'Nom (A → Z)' },
		{ value: 'lastRun', label: 'Dernière exécution' }
	];

	function setStatus(status: StatusFilter): void {
		workflowFilters.update((f) => ({ ...f, status }));
	}

	function toggleTag(tag: string): void {
		workflowFilters.update((f) => ({
			...f,
			tags: f.tags.includes(tag) ? f.tags.filter((t) => t !== tag) : [...f.tags, tag]
		}));
	}

	function clearQuery(): void {
		workflowFilters.update((f) => ({ ...f, query: '' }));
	}
</script>

<div class="toolbar">
	<div class="search">
		<span class="search-icon" aria-hidden="true">🔍</span>
		<input
			type="search"
			placeholder="Rechercher un workflow ou un tag…"
			aria-label="Rechercher un workflow"
			bind:value={$workflowFilters.query}
		/>
		{#if $workflowFilters.query}
			<button type="button" class="clear" on:click={clearQuery} aria-label="Effacer la recherche">×</button>
		{/if}
	</div>

	<div class="chips" role="group" aria-label="Filtrer par statut">
		{#each statusOptions as opt (opt.value)}
			<button
				type="button"
				class="chip"
				class:active={$workflowFilters.status === opt.value}
				class:danger={opt.value === 'error' && counts.error > 0}
				on:click={() => setStatus(opt.value)}
			>
				{opt.label}
				<span class="count">{counts[opt.value]}</span>
			</button>
		{/each}
	</div>

	{#if tagNames.length > 0}
		<div class="chips tags" role="group" aria-label="Filtrer par tag">
			{#each tagNames as tag (tag)}
				<button
					type="button"
					class="chip tag"
					class:active={$workflowFilters.tags.includes(tag)}
					on:click={() => toggleTag(tag)}
				>
					{tag}
				</button>
			{/each}
		</div>
	{/if}

	<label class="sort">
		<span class="muted">Trier par</span>
		<select bind:value={$workflowFilters.sort}>
			{#each sortOptions as opt (opt.value)}
				<option value={opt.value}>{opt.label}</option>
			{/each}
		</select>
	</label>
</div>

<style>
	.toolbar {
		margin-bottom: 14px;
		display: flex;
		flex-direction: column;
		gap: 10px;
	}
	.search {
		position: relative;
		display: flex;
		align-items: center;
	}
	.search-icon {
		position: absolute;
		left: 12px;
		opacity: 0.6;
		font-size: 0.9rem;
		pointer-events: none;
	}
	.search input {
		padding-left: 34px;
		padding-right: 34px;
	}
	.search input::-webkit-search-cancel-button {
		display: none;
	}
	.clear {
		position: absolute;
		right: 4px;
		background: transparent;
		border: none;
		color: var(--text-muted);
		font-size: 1.2rem;
		line-height: 1;
		padding: 8px;
		cursor: pointer;
	}
	.chips {
		display: flex;
		gap: 8px;
		overflow-x: auto;
		padding-bottom: 2px;
		scrollbar-width: none;
	}
	.chips::-webkit-scrollbar {
		display: none;
	}
	.chip {
		flex: 0 0 auto;
		display: inline-flex;
		align-items: center;
		gap: 6px;
		background: var(--bg-card);
		border: 1px solid var(--border);
		color: var(--text-muted);
		border-radius: 999px;
		padding: 7px 12px;
		font-size: 0.82rem;
		font-weight: 600;
		cursor: pointer;
		white-space: nowrap;
	}
	.chip .count {
		background: var(--bg-elevated);
		border-radius: 999px;
		padding: 1px 6px;
		font-size: 0.72rem;
		color: var(--text-muted);
	}
	.chip.active {
		border-color: var(--accent);
		color: var(--text);
		background: var(--accent-soft);
	}
	.chip.active .count {
		color: var(--text);
	}
	.chip.danger.active {
		border-color: var(--error);
		background: var(--error-soft);
	}
	.chip.tag {
		font-weight: 500;
	}
	.sort {
		align-self: flex-end;
		display: flex;
		align-items: center;
		gap: 8px;
	}
	.sort select {
		width: auto;
		padding: 6px 10px;
		font-size: 0.82rem;
	}
</style>
