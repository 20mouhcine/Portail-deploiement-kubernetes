import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthService } from '../../../../core/auth/services/auth.service';
import { DeploymentFormValue } from '../../../../core/deployments/models/deployment.models';
import { DeploymentsService } from '../../../../core/deployments/services/deployments.service';
import { ActionHistoryService } from '../../../../core/history/services/action-history.service';
import Project from '../../../../core/projects/models/projects.model';
import { ProjectsService } from '../../../../core/projects/services/projects.service';
import { StatusCard } from '../../../../shared/components/status-card/status-card';
import { DeploymentFormModal } from '../../../deployments/components/deployment-form-modal/deployment-form-modal';
import { ActionTypeBadge } from '../../../history/components/action-type-badge/action-type-badge';

@Component({
  selector: 'app-dashboard-page',
  imports: [DatePipe, RouterLink, StatusCard, DeploymentFormModal, ActionTypeBadge],
  templateUrl: './dashboard-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardPage {
  private readonly auth = inject(AuthService);
  private readonly deploymentsService = inject(DeploymentsService);
  private readonly projectsService = inject(ProjectsService);
  private readonly historyService = inject(ActionHistoryService);

  protected readonly user = this.auth.user;
  protected readonly deployments = toSignal(this.deploymentsService.deployments$, { initialValue: [] });
  protected readonly projects = signal<readonly Project[]>([]);
  protected readonly history = this.historyService.entries;
  protected readonly historyError = this.historyService.error;
  protected readonly loading = computed(() => this.deploymentsService.loading() || this.historyService.loading());
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly formOpen = signal(false);
  protected readonly actionInProgress = signal(false);
  protected readonly canCreate = computed(() => this.auth.hasRole('DEVOPS'));
  protected readonly activeDeployments = computed(() => this.deployments().filter(item => item.status === 'RUNNING').length);
  protected readonly failedDeployments = computed(() => this.deployments().filter(item => item.status === 'FAILED').length);
  protected readonly recentHistory = computed(() => this.history().slice(0, 5));
  protected readonly emptyDeployment: DeploymentFormValue = {
    projectId: '', projectName: '', name: '', namespace: 'default', replicas: 1,
    image: '', port: 8080, cpu: '250m', memory: '256Mi', envVariables: {}, secretVariables: {},
  };

  constructor() { this.reload(); }

  protected reload(): void {
    this.errorMessage.set(null);
    this.deploymentsService.refresh();
    this.historyService.load();
    this.projectsService.loadProjects().subscribe({
      next: (projects) => this.projects.set(projects),
      error: (error) => this.errorMessage.set(this.auth.errorMessage(error)),
    });
  }

  protected openDeploymentForm(): void { this.errorMessage.set(null); this.formOpen.set(true); }
  protected closeDeploymentForm(): void { this.formOpen.set(false); }

  protected createDeployment(value: DeploymentFormValue): void {
    if (this.actionInProgress()) return;
    this.actionInProgress.set(true);
    this.errorMessage.set(null);
    this.deploymentsService.create(value).pipe(finalize(() => this.actionInProgress.set(false))).subscribe({
      next: () => { this.formOpen.set(false); this.historyService.load(); },
      error: (error) => this.errorMessage.set(this.auth.errorMessage(error)),
    });
  }
}
