// Types miroir du strict nécessaire de l'API REST n8n (https://docs.n8n.io/api/).
// On ne mappe que les champs consommés par l'app, pas le schéma complet.

export interface N8nWorkflow {
	id: string;
	name: string;
	active: boolean;
	updatedAt: string;
	createdAt: string;
	tags?: { id: string; name: string }[];
	nodes?: N8nWorkflowNode[];
}

export interface N8nWorkflowNode {
	id: string;
	name: string;
	type: string;
	// Pour détecter si le workflow attend des paramètres au déclenchement manuel
	// (ex: un node "Set" juste après un Manual Trigger, ou un Webhook avec un schéma défini).
	parameters?: Record<string, unknown>;
}

export type ExecutionStatus = 'success' | 'error' | 'running' | 'waiting' | 'canceled';

export interface N8nExecution {
	id: string;
	workflowId: string;
	finished: boolean;
	mode: string;
	status: ExecutionStatus;
	startedAt: string;
	stoppedAt: string | null;
	// Résumé d'erreur extrait côté client depuis execution.data (pas le JSON brut affiché à l'utilisateur)
	errorSummary?: ExecutionErrorSummary;
}

export interface ExecutionErrorSummary {
	nodeName: string;
	message: string;
	timestamp: string;
}

export interface N8nExecutionListResponse {
	data: N8nExecution[];
	nextCursor: string | null;
}

export interface N8nWorkflowListResponse {
	data: N8nWorkflow[];
	nextCursor: string | null;
}

export interface WorkflowWithLastRun extends N8nWorkflow {
	lastExecution?: N8nExecution;
}

export interface HealthSummary {
	failuresLast24h: number;
	staleWorkflows: { id: string; name: string; lastRunAt: string | null; daysSinceLastRun: number }[];
	generatedAt: string;
}
