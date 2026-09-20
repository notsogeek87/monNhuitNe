import { config } from './config.js';
import { fetchAllWorkflows, fetchLastExecutionPerWorkflow, fetchRecentFailedExecutions } from './n8nClient.js';
import { notifyFailure } from './push.js';

export interface HealthSummary {
	failuresLast24h: number;
	staleWorkflows: { id: string; name: string; lastRunAt: string | null; daysSinceLastRun: number }[];
	generatedAt: string;
}

// IDs d'exécution déjà notifiés (par le webhook d'erreur n8n ou par ce poller
// lui-même), pour ne jamais doublonner une notification entre les deux voies.
const notifiedExecutionIds = new Set<string>();

export function markExecutionNotified(executionId: string): void {
	notifiedExecutionIds.add(executionId);
}

export async function computeHealthSummary(apiKey: string): Promise<HealthSummary> {
	const since24h = new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString();
	const [workflows, recentFailures] = await Promise.all([
		fetchAllWorkflows(apiKey),
		fetchRecentFailedExecutions(apiKey, since24h)
	]);

	const activeWorkflows = workflows.filter((w) => w.active);
	const staleThresholdMs = config.staleWorkflowDays * 24 * 60 * 60 * 1000;

	const staleWorkflows = (
		await Promise.all(
			activeWorkflows.map(async (wf) => {
				const last = await fetchLastExecutionPerWorkflow(apiKey, wf.id).catch(() => undefined);
				const lastRunAt = last?.startedAt ?? null;
				const daysSinceLastRun = lastRunAt
					? Math.floor((Date.now() - new Date(lastRunAt).getTime()) / (24 * 60 * 60 * 1000))
					: Infinity;
				return { id: wf.id, name: wf.name, lastRunAt, daysSinceLastRun };
			})
		)
	).filter((wf) => wf.daysSinceLastRun * 24 * 60 * 60 * 1000 >= staleThresholdMs);

	return {
		failuresLast24h: recentFailures.length,
		staleWorkflows,
		generatedAt: new Date().toISOString()
	};
}

/**
 * Sondage de secours (5-10 min) : couvre les échecs qui ne seraient pas passés
 * par l'Error Workflow (pas encore configuré sur tous les workflows, ou n8n
 * indisponible au moment de l'échec). Ne notifie que les executions pas
 * déjà vues par `markExecutionNotified`.
 */
export function startHealthPoller(apiKey: string): NodeJS.Timeout {
	const intervalMs = config.healthPollIntervalMinutes * 60 * 1000;

	const tick = async () => {
		try {
			const since = new Date(Date.now() - intervalMs * 2).toISOString();
			const failures = await fetchRecentFailedExecutions(apiKey, since);
			for (const failure of failures) {
				if (notifiedExecutionIds.has(failure.id)) continue;
				markExecutionNotified(failure.id);
				await notifyFailure({
					title: 'Échec de workflow (détecté par sondage)',
					body: `Workflow ${failure.workflowId} en échec.`,
					workflowId: failure.workflowId
				});
			}
		} catch (err) {
			console.error('[healthPoll] échec du sondage:', err);
		}
	};

	tick();
	return setInterval(tick, intervalMs);
}
