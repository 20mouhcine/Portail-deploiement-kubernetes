import { Injectable, signal } from '@angular/core';

import { Deployment, DeploymentFormValue } from '../models/deployment.models';

@Injectable({ providedIn: 'root' })
export class DeploymentsService {
  // Données temporaires : ces méthodes seront remplacées par des appels HttpClient.
  private readonly deploymentState = signal<readonly Deployment[]>([
    {
      id: 'demo-api',
      projectName: 'Portail Kubernetes',
      name: 'backend-api',
      status: 'RUNNING',
      namespace: 'production',
      replicas: 3,
      image: 'kube-portal/backend:1.4.0',
      port: 8080,
      cpu: '500m',
      memory: '512Mi',
      createdAt: '2026-07-20T10:30:00Z',
      deployedBy: 'devops.demo',
    },
    {
      id: 'demo-frontend',
      projectName: 'Portail Kubernetes',
      name: 'frontend-web',
      status: 'SUCCEEDED',
      namespace: 'staging',
      replicas: 2,
      image: 'kube-portal/frontend:1.4.0',
      port: 80,
      cpu: '250m',
      memory: '256Mi',
      createdAt: '2026-07-19T14:15:00Z',
      deployedBy: 'devops.demo',
    },
    {
      id: 'demo-worker',
      projectName: 'Service Notifications',
      name: 'notification-worker',
      status: 'FAILED',
      namespace: 'development',
      replicas: 1,
      image: 'kube-portal/notifications:0.8.2',
      port: 8090,
      cpu: '300m',
      memory: '384Mi',
      createdAt: '2026-07-18T09:05:00Z',
      deployedBy: 'devops.demo',
    },
  ]);

  readonly deployments = this.deploymentState.asReadonly();

  create(value: DeploymentFormValue, username: string): void {
    const deployment: Deployment = {
      ...value,
      id: crypto.randomUUID(),
      status: 'PENDING',
      createdAt: new Date().toISOString(),
      deployedBy: username,
    };
    this.deploymentState.update((deployments) => [deployment, ...deployments]);
  }

  update(id: string, value: DeploymentFormValue): void {
    this.patch(id, value);
  }

  restart(id: string): void {
    this.patch(id, { status: 'RUNNING' });
  }

  stop(id: string): void {
    this.patch(id, { status: 'STOPPED' });
  }

  scale(id: string, replicas: number): void {
    this.patch(id, { replicas: Math.max(1, replicas) });
  }

  delete(id: string): void {
    this.deploymentState.update((deployments) => deployments.filter((item) => item.id !== id));
  }

  private patch(id: string, changes: Partial<Deployment>): void {
    this.deploymentState.update((deployments) =>
      deployments.map((item) => item.id === id ? { ...item, ...changes } : item)
    );
  }
}
