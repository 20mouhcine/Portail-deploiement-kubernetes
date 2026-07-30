export type DeploymentEventLevel = 'INFO' | 'SUCCESS' | 'WARNING' | 'ERROR';
export type DeploymentEventSource = 'SYSTEM' | 'KUBERNETES' | 'CONTAINER';

export interface DeploymentEvent {
  id: string;
  deploymentId: string;
  timestamp: string;
  level: DeploymentEventLevel;
  source: DeploymentEventSource;
  message: string;
}

export interface DeploymentStatusChange {
  deploymentId: string;
  status: string;
}
