export type DeploymentStatus = 'PENDING' | 'RUNNING' | 'FAILED' | 'STOPPED';

export type DeploymentEventLevel = 'INFO' | 'SUCCESS' | 'WARNING' | 'ERROR';
export type DeploymentEventSource = 'SYSTEM' | 'KUBERNETES' | 'CONTAINER';

export type JobStatus = 'QUEUED' | 'APPLYING' | 'ROLLING_OUT' | 'READY' | 'FAILED';
export type JobOperationType = 'CREATE' | 'UPDATE' | 'SCALE' | 'DELETE' | 'RESTART';

export interface DeploymentJobResponse {
  readonly id: string;
  readonly deploymentId: string;
  readonly operationType: JobOperationType;
  readonly status: JobStatus;
  readonly retryCount: number;
  readonly errorMessage: string | null;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface DeploymentPod {
  readonly name: string;
  readonly status: string;
  readonly phase: string;
  readonly node: string | null;
  readonly restartCount: number;
  readonly ready: boolean;
  readonly reason: string | null;
  readonly createdAt: string | null;
}

export interface DeploymentEvent {
  readonly id: string;
  readonly timestamp: string;
  readonly level: DeploymentEventLevel;
  readonly source: DeploymentEventSource;
  readonly message: string;
}

export interface DeploymentRevision {
  readonly id: string;
  readonly revisionNumber: number;
  readonly image: string;
  readonly replicas: number;
  readonly port: number;
  readonly cpu: string;
  readonly memory: string;
  // readonly gitCommit: string | null;
  // readonly gitTag: string | null;
  readonly createdAt: string;
  readonly envVariables: Record<string, string>;
  readonly secretVariables: Record<string, string>;
  // readonly requestedHostname: string | null;
  // readonly requestedPath: string | null;
  // readonly tlsEnabled: boolean | null;
  // readonly tlsSecretName: string | null;
}

export interface EnvVariable {
  readonly key: string;
  readonly value: string;
}

export interface Deployment {
  readonly id: string;
  readonly projectId?: string;
  readonly projectName: string;
  readonly name: string;
  readonly status: DeploymentStatus;
  readonly namespace: string;
  readonly replicas: number;
  readonly image: string;
  readonly port: number;
  readonly cpu: string;
  readonly memory: string;
  readonly accessUrl: string;
  readonly createdAt: string;
  readonly deployedBy: string;
  readonly envVariables?: Record<string, string>;
  readonly secretVariables?: Record<string, string>;
  readonly gitRepository?: string | null;
  readonly gitBranch?: string | null;
  readonly gitCommit?: string | null;
  readonly gitTag?: string | null;
  readonly requestedHostname?: string | null;
  readonly requestedPath?: string | null;
  readonly tlsEnabled?: boolean;
  readonly tlsSecretName?: string | null;
  readonly desiredReplicas?: number;
  readonly availableReplicas?: number;
  readonly readyReplicas?: number;
  readonly unavailableReplicas?: number;
  readonly failureCause?: string | null;
  readonly pods?: DeploymentPod[];
  readonly events?: DeploymentEvent[];
  readonly rolloutHistory?: DeploymentRevision[];
  readonly operationId?: string;
}

export interface DeploymentDetail extends Deployment {
  readonly configVariables?: Record<string, string>;
  readonly secretKeys?: readonly string[];
  readonly logs?: string;
}

export interface DeploymentFormValue {
  readonly projectId: string;
  readonly projectName: string;
  readonly name: string;
  readonly namespace: string;
  readonly replicas: number;
  readonly image: string;
  readonly port: number;
  readonly cpu: string;
  readonly memory: string;
  readonly envVariables: Record<string, string>;
  readonly secretVariables: Record<string, string>;
  // readonly gitRepository: string;
  // readonly gitBranch: string;
  // readonly gitCommit: string;
  // readonly gitTag: string;
  // readonly requestedHostname: string;
  // readonly requestedPath: string;
  // readonly tlsEnabled: boolean;
  // readonly tlsSecretName: string;
}

