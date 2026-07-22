export type DeploymentStatus = 'PENDING' | 'RUNNING' | 'FAILED' | 'SUCCEEDED' | 'STOPPED';

export interface Deployment {
  readonly id: string;
  readonly projectName: string;
  readonly name: string;
  readonly status: DeploymentStatus;
  readonly namespace: string;
  readonly replicas: number;
  readonly image: string;
  readonly port: number;
  readonly cpu: string;
  readonly memory: string;
  readonly createdAt: string;
  readonly deployedBy: string;
}

export interface DeploymentFormValue {
  readonly projectName: string;
  readonly name: string;
  readonly namespace: string;
  readonly replicas: number;
  readonly image: string;
  readonly port: number;
  readonly cpu: string;
  readonly memory: string;
}
