import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { finalize, Observable } from 'rxjs';
import { toSignal } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';

import { AuthService } from '../../../../core/auth/services/auth.service';
import { Deployment, DeploymentFormValue, DeploymentStatus } from '../../../../core/deployments/models/deployment.models';
import { DeploymentsService } from '../../../../core/deployments/services/deployments.service';
import { StatusCard } from '../../../../shared/components/status-card/status-card';
import { ConfirmDeleteModal } from '../../../projects/components/confirm-delete-modal/confirm-delete-modal';
import { DeploymentCard } from '../../components/deployment-card/deployment-card';
import { DeploymentFormModal } from '../../components/deployment-form-modal/deployment-form-modal';
import { DeploymentLogsModalComponent } from '../../components/deployment-logs-modal/deployment-logs-modal';

@Component({
  selector: 'app-deployments-page',
  imports: [StatusCard, DeploymentCard, DeploymentFormModal, ConfirmDeleteModal, DeploymentLogsModalComponent],
  templateUrl: './deployments-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DeploymentsPage {
  private readonly auth = inject(AuthService);
  private readonly service = inject(DeploymentsService);
  private readonly router = inject(Router);

  protected readonly user = this.auth.user;
  protected readonly deployments = toSignal(this.service.deployments$, { initialValue: [] });
  protected readonly searchTerm = signal('');
  protected readonly statusFilter = signal<DeploymentStatus | 'ALL'>('ALL');
  protected readonly formOpen = signal(false);
  protected readonly formMode = signal<'create' | 'edit'>('create');
  protected readonly editingDeployment = signal<Deployment | null>(null);
  protected readonly deploymentToDelete = signal<Deployment | null>(null);
  protected readonly logsDeployment = signal<Deployment | null>(null);
  protected readonly actionError = signal<string | null>(null);
  protected readonly actionInProgress = signal(false);
  protected readonly loading = this.service.loading;
  protected readonly loadError = this.service.loadError;
  protected readonly activeOperations = signal<Set<string>>(new Set());

  protected readonly canCreate = computed(() => (this.user()?.roles.includes('DEVOPS') || this.user()?.roles.includes('ADMIN')) ?? false);
  protected readonly canManage = computed(() =>
    this.user()?.roles.some((role) => role === 'ADMIN' || role === 'DEVOPS') ?? false,
  );
  protected readonly runningCount = computed(() => this.deployments().filter((item) => item.status === 'RUNNING').length);
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
      projectId: deployment.projectId ?? '', projectName: deployment.projectName, name: deployment.name,
      namespace: deployment.namespace, replicas: deployment.replicas, image: deployment.image,
      port: deployment.port, cpu: deployment.cpu, memory: deployment.memory,
      envVariables: deployment.envVariables ?? {},
      secretVariables: deployment.secretVariables ?? {},
      gitRepository: deployment.gitRepository ?? '',
      gitBranch: deployment.gitBranch ?? '',
      gitCommit: deployment.gitCommit ?? '',
      gitTag: deployment.gitTag ?? '',
      requestedHostname: deployment.requestedHostname ?? '',
      requestedPath: deployment.requestedPath ?? '',
      tlsEnabled: deployment.tlsEnabled ?? false,
      tlsSecretName: deployment.tlsSecretName ?? '',
    } : {
      projectId: '', projectName: '', name: '', namespace: 'default', replicas: 1,
      image: '', port: 8080, cpu: '250m', memory: '256Mi', envVariables: {},
      secretVariables: {}, gitRepository: '', gitBranch: '', gitCommit: '', gitTag: '',
      requestedHostname: '', requestedPath: '', tlsEnabled: false, tlsSecretName: '',
    };
  });

  protected search(event: Event): void { this.searchTerm.set((event.target as HTMLInputElement).value); }
  protected filterByStatus(event: Event): void { this.statusFilter.set((event.target as HTMLSelectElement).value as DeploymentStatus | 'ALL'); }
  protected openCreate(): void { this.clearActionError(); this.formMode.set('create'); this.editingDeployment.set(null); this.formOpen.set(true); }
  protected openEdit(deployment: Deployment): void { this.clearActionError(); this.formMode.set('edit'); this.editingDeployment.set(deployment); this.formOpen.set(true); }
  protected openDetails(deployment: Deployment): void { void this.router.navigate(['/deployments', deployment.id]); }
  protected closeForm(): void { this.formOpen.set(false); this.editingDeployment.set(null); }
  protected openLogs(deployment: Deployment): void { this.logsDeployment.set(deployment); }
  protected closeLogs(): void { this.logsDeployment.set(null); }
  protected clearActionError(): void { this.actionError.set(null); }
  protected reload(): void { this.service.refresh(); }

  protected save(value: DeploymentFormValue): void {
    const current = this.editingDeployment();
    const request = this.formMode() === 'edit' && current
      ? this.service.update(current.id, value)
      : this.service.create(value);
    this.runAction(request, () => this.closeForm());
  }

  protected restart(deployment: Deployment): void { this.runAction(this.service.restart(deployment.id)); }
  protected stop(deployment: Deployment): void { this.runAction(this.service.stop(deployment.id)); }
  protected scale(event: { deployment: Deployment; replicas: number }): void {
    this.runAction(this.service.scale(event.deployment.id, event.replicas));
  }
  protected askDelete(deployment: Deployment): void { this.clearActionError(); this.deploymentToDelete.set(deployment); }
  protected cancelDelete(): void { this.deploymentToDelete.set(null); }
  protected confirmDelete(): void {
    const deployment = this.deploymentToDelete();
    if (deployment) {
      this.runAction(this.service.delete(deployment.id), () => this.cancelDelete());
    }
  }

  private runAction(operation: Observable<unknown>, onSuccess?: () => void): void {
    if (this.actionInProgress()) return;
    this.clearActionError();
    this.actionInProgress.set(true);
    operation.pipe(finalize(() => this.actionInProgress.set(false))).subscribe({
      next: (result) => {
        onSuccess?.();
        if (result && typeof result === 'object' && 'operationId' in result && result.operationId) {
          this.trackOperation(result as Deployment);
        }
      },
      error: (error) => this.actionError.set(this.auth.errorMessage(error)),
    });
  }

  private trackOperation(deployment: Deployment): void {
    if (!deployment.operationId) return;

    this.activeOperations.update(set => {
      const newSet = new Set(set);
      newSet.add(deployment.id);
      return newSet;
    });

    this.service.waitForOperation(deployment.operationId).subscribe({
      next: (job) => {
        this.activeOperations.update(set => {
          const newSet = new Set(set);
          newSet.delete(deployment.id);
          return newSet;
        });

        if (job.status === 'FAILED') {
          this.actionError.set(job.errorMessage || 'Operation failed');
        } else {
          // Success, relying on SSE to update the list, but we can also trigger a manual refresh
          // this.service['refresh'](); // optional
        }
      },
      error: () => {
        this.activeOperations.update(set => {
          const newSet = new Set(set);
          newSet.delete(deployment.id);
          return newSet;
        });
        this.actionError.set('Le suivi de l’opération a été interrompu. Réessayez.');
      }
    });
  }
}
