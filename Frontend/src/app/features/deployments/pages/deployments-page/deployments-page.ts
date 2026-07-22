import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';

import { AuthService } from '../../../../core/auth/services/auth.service';
import { Deployment, DeploymentFormValue, DeploymentStatus } from '../../../../core/deployments/models/deployment.models';
import { DeploymentsService } from '../../../../core/deployments/services/deployments.service';
import { StatusCard } from '../../../../shared/components/status-card/status-card';
import { ConfirmDeleteModal } from '../../../projects/components/confirm-delete-modal/confirm-delete-modal';
import { DeploymentCard } from '../../components/deployment-card/deployment-card';
import { DeploymentFormModal } from '../../components/deployment-form-modal/deployment-form-modal';

@Component({
  selector: 'app-deployments-page',
  imports: [StatusCard, DeploymentCard, DeploymentFormModal, ConfirmDeleteModal],
  templateUrl: './deployments-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DeploymentsPage {
  private readonly auth = inject(AuthService);
  private readonly service = inject(DeploymentsService);

  protected readonly user = this.auth.user;
  protected readonly deployments = this.service.deployments;
  protected readonly searchTerm = signal('');
  protected readonly statusFilter = signal<DeploymentStatus | 'ALL'>('ALL');
  protected readonly formOpen = signal(false);
  protected readonly formMode = signal<'create' | 'edit'>('create');
  protected readonly editingDeployment = signal<Deployment | null>(null);
  protected readonly deploymentToDelete = signal<Deployment | null>(null);

  protected readonly canCreate = computed(() => this.user()?.roles.includes('DEVOPS') ?? false);
  protected readonly canManage = computed(() =>
    this.user()?.roles.some((role) => role === 'ADMIN' || role === 'DEVOPS') ?? false
  );
  protected readonly runningCount = computed(() => this.deployments().filter((item) => item.status === 'RUNNING' || item.status === 'SUCCEEDED').length);
  protected readonly failedCount = computed(() => this.deployments().filter((item) => item.status === 'FAILED').length);
  protected readonly totalReplicas = computed(() => this.deployments().reduce((total, item) => total + item.replicas, 0));
  protected readonly filteredDeployments = computed(() => {
    const term = this.searchTerm().trim().toLocaleLowerCase('fr');
    const status = this.statusFilter();
    return this.deployments().filter((deployment) => {
      const matchesStatus = status === 'ALL' || deployment.status === status;
      const matchesSearch = !term || [deployment.name, deployment.projectName, deployment.namespace, deployment.image]
        .some((value) => value.toLocaleLowerCase('fr').includes(term));
      return matchesStatus && matchesSearch;
    });
  });
  protected readonly formValue = computed<DeploymentFormValue>(() => {
    const deployment = this.editingDeployment();
    return deployment ? {
      projectName: deployment.projectName, name: deployment.name,
      namespace: deployment.namespace, replicas: deployment.replicas,
      image: deployment.image, port: deployment.port,
      cpu: deployment.cpu, memory: deployment.memory,
    } : {
      projectName: '', name: '', namespace: 'default', replicas: 1,
      image: '', port: 8080, cpu: '250m', memory: '256Mi',
    };
  });

  protected search(event: Event): void { this.searchTerm.set((event.target as HTMLInputElement).value); }
  protected filterByStatus(event: Event): void { this.statusFilter.set((event.target as HTMLSelectElement).value as DeploymentStatus | 'ALL'); }
  protected openCreate(): void { this.formMode.set('create'); this.editingDeployment.set(null); this.formOpen.set(true); }
  protected openEdit(deployment: Deployment): void { this.formMode.set('edit'); this.editingDeployment.set(deployment); this.formOpen.set(true); }
  protected closeForm(): void { this.formOpen.set(false); this.editingDeployment.set(null); }

  protected save(value: DeploymentFormValue): void {
    const current = this.editingDeployment();
    if (this.formMode() === 'edit' && current) this.service.update(current.id, value);
    else this.service.create(value, this.user()?.username ?? 'utilisateur');
    this.closeForm();
  }

  protected restart(deployment: Deployment): void { this.service.restart(deployment.id); }
  protected stop(deployment: Deployment): void { this.service.stop(deployment.id); }
  protected scale(event: { deployment: Deployment; replicas: number }): void { this.service.scale(event.deployment.id, event.replicas); }
  protected askDelete(deployment: Deployment): void { this.deploymentToDelete.set(deployment); }
  protected cancelDelete(): void { this.deploymentToDelete.set(null); }
  protected confirmDelete(): void { const deployment = this.deploymentToDelete(); if (deployment) this.service.delete(deployment.id); this.cancelDelete(); }
}
