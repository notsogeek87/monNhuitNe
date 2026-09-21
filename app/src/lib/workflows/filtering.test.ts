import { describe, expect, it } from 'vitest';
import type { WorkflowWithLastRun } from '../api/types';
import {
	collectTagNames,
	computeStatusCounts,
	defaultWorkflowFilters,
	filterAndSortWorkflows,
	matchesQuery,
	matchesStatus,
	matchesTags,
	sortWorkflows
} from './filtering';

function workflow(overrides: Partial<WorkflowWithLastRun>): WorkflowWithLastRun {
	return {
		id: overrides.id ?? 'wf-1',
		name: overrides.name ?? 'Workflow',
		active: overrides.active ?? true,
		createdAt: '2024-01-01T00:00:00.000Z',
		updatedAt: '2024-01-01T00:00:00.000Z',
		...overrides
	};
}

const errorWf = workflow({
	id: 'err',
	name: 'Sync factures',
	tags: [{ id: 't1', name: 'compta' }],
	lastExecution: { id: 'e1', workflowId: 'err', finished: true, mode: 'trigger', status: 'error', startedAt: '2024-06-01T10:00:00.000Z', stoppedAt: '2024-06-01T10:00:05.000Z' }
});
const successWf = workflow({
	id: 'ok',
	name: 'Backup nightly',
	tags: [{ id: 't2', name: 'infra' }],
	lastExecution: { id: 'e2', workflowId: 'ok', finished: true, mode: 'trigger', status: 'success', startedAt: '2024-06-02T10:00:00.000Z', stoppedAt: '2024-06-02T10:00:05.000Z' }
});
const neverRunWf = workflow({ id: 'never', name: 'Alerte stock', active: true });
const inactiveWf = workflow({ id: 'inactive', name: 'Ancien import', active: false });

const allWorkflows = [errorWf, successWf, neverRunWf, inactiveWf];

describe('matchesStatus', () => {
	it('accepte tout avec le filtre "all"', () => {
		expect(matchesStatus(inactiveWf, 'all')).toBe(true);
	});
	it('filtre les workflows en échec', () => {
		expect(matchesStatus(errorWf, 'error')).toBe(true);
		expect(matchesStatus(successWf, 'error')).toBe(false);
	});
	it('filtre les workflows jamais exécutés', () => {
		expect(matchesStatus(neverRunWf, 'never')).toBe(true);
		expect(matchesStatus(successWf, 'never')).toBe(false);
	});
	it('filtre par actif/inactif', () => {
		expect(matchesStatus(inactiveWf, 'inactive')).toBe(true);
		expect(matchesStatus(inactiveWf, 'active')).toBe(false);
	});
});

describe('matchesQuery', () => {
	it('recherche insensible à la casse sur le nom', () => {
		expect(matchesQuery(errorWf, 'FACTURES')).toBe(true);
		expect(matchesQuery(errorWf, 'backup')).toBe(false);
	});
	it('recherche aussi dans les tags', () => {
		expect(matchesQuery(errorWf, 'compta')).toBe(true);
	});
	it('accepte tout avec une recherche vide', () => {
		expect(matchesQuery(errorWf, '   ')).toBe(true);
	});
});

describe('matchesTags', () => {
	it('accepte tout sans tag sélectionné', () => {
		expect(matchesTags(errorWf, [])).toBe(true);
	});
	it("matche si le workflow a au moins un des tags sélectionnés", () => {
		expect(matchesTags(errorWf, ['compta', 'infra'])).toBe(true);
		expect(matchesTags(successWf, ['compta'])).toBe(false);
	});
});

describe('sortWorkflows', () => {
	it('trie par nom alphabétique', () => {
		const sorted = sortWorkflows(allWorkflows, 'name');
		expect(sorted.map((w) => w.name)).toEqual(['Alerte stock', 'Ancien import', 'Backup nightly', 'Sync factures']);
	});

	it('trie par priorité : échec puis jamais exécuté (actif) puis sain puis inactif', () => {
		const sorted = sortWorkflows(allWorkflows, 'priority');
		expect(sorted.map((w) => w.id)).toEqual(['err', 'never', 'ok', 'inactive']);
	});

	it('trie par dernière exécution, la plus récente en premier, jamais exécuté en dernier', () => {
		const sorted = sortWorkflows(allWorkflows, 'lastRun');
		expect(sorted.map((w) => w.id)).toEqual(['ok', 'err', 'never', 'inactive']);
	});
});

describe('computeStatusCounts', () => {
	it('compte chaque catégorie indépendamment', () => {
		expect(computeStatusCounts(allWorkflows)).toEqual({
			all: 4,
			active: 3,
			inactive: 1,
			error: 1,
			never: 2
		});
	});
});

describe('collectTagNames', () => {
	it('renvoie les tags uniques triés', () => {
		expect(collectTagNames(allWorkflows)).toEqual(['compta', 'infra']);
	});
});

describe('filterAndSortWorkflows', () => {
	it('combine filtre texte, statut et tri', () => {
		const result = filterAndSortWorkflows(allWorkflows, {
			...defaultWorkflowFilters,
			status: 'active',
			sort: 'name'
		});
		expect(result.map((w) => w.id)).toEqual(['never', 'ok', 'err']);
	});
});
