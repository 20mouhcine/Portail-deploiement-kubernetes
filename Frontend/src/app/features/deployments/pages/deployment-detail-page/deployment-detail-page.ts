import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthService } from '../../../../core/auth/services/auth.service';
import {
  DeploymentDetail,
  DeploymentFormValue,
  DeploymentRevision,
} from '../../../../core/deployments/models/deployment.models';
import { DeploymentsService } from '../../../../core/deployments/services/deployments.service';
import { DeploymentStatusBadge } from '../../components/deployment-status-badge/deployment-status-badge';

@Component({
  selector: 'app-deployment-detail-page',
  standalone: true,
  imports: [CommonModule, DeploymentStatusBadge],
  templateUrl: './deployment-detail-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DeploymentDetailPage {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  private readonly service = inject(DeploymentsService);

  protected readonly deployment = signal<DeploymentDetail | null>(null);
  protected readonly logs = signal('');
  protected readonly loading = signal(true);
  protected readonly actionInProgress = signal(false);
  protected readonly actionError = signal<string | null>(null);
  protected readonly newImage = signal('');

  protected readonly user = this.auth.user;
  protected readonly canManage = computed(() => this.user()?.roles.some((role) => role === 'ADMIN' || role === 'DEVOPS') ?? false);
  protected readonly secretKeys = computed(() => this.deployment()?.secretKeys ?? []);
  protected readonly configEntries = computed(() => Object.entries(this.deployment()?.configVariables ?? this.deployment()?.envVariables ?? {}));
  protected readonly latestRevision = computed(() => this.deployment()?.rolloutHistory?.[0] ?? null);
  protected readonly previousRevision = computed<DeploymentRevision | null>(() => this.deployment()?.rolloutHistory?.[1] ?? null);

  ngOnInit(): void {
    const deploymentId = this.route.snapshot.paramMap.get('id');
    if (!deploymentId) {
      void this.router.navigate(['/deployments']);
      return;
    }

    this.load(deploymentId);
  }

  protected goBack(): void {
    void this.router.navigate(['/deployments']);
  }

  protected reload(): void {
    const deploymentId = this.deployment()?.id;
    if (deploymentId) {
      this.load(deploymentId);
    }
  }

  protected deployNewImage(): void {
    const current = this.deployment();
    const image = this.newImage().trim();
    if (!current || !image) {
      this.actionError.set('Indiquez une image avant de lancer le dÃ©ploiement.');
      return;
    }

    this.runAction(
      this.service.update(current.id, this.buildRequest(current, { image })),
      () => {
        this.newImage.set('');
        this.reload();
      },
    );
  }
  copyToClipboard(text: string): void {
    navigator.clipboard.writeText(text)}

  protected rollback(): void {
    const current = this.deployment();
    if (!current) {
      return;
    }

    this.runAction(this.service.rollback(current.id), () => this.reload());
  }

  protected cancelRollout(): void {
    const current = this.deployment();
    if (!current) {
      return;
    }

    this.runAction(this.service.stop(current.id), () => this.reload());
  }

  protected retryFailure(): void {
    const current = this.deployment();
    if (!current) {
      return;
    }

    this.runAction(this.service.restart(current.id), () => this.reload());
  }

  protected statusLabel(status: string): string {
    const labels: Record<string, string> = {
      PENDING: 'En progression',
      RUNNING: 'Actif',
      FAILED: 'En Ã©chec',
      STOPPED: 'ArrÃªtÃ©',
      SUCCEEDED: 'Stable',
    };
    return labels[status] ?? status;
  }

  protected maskSecret(value: string): string {
    return value ? 'â€¢â€¢â€¢â€¢â€¢â€¢â€¢â€¢' : 'â€”';
  }

  protected revisionBadge(revision: DeploymentRevision): string {
    return `R${revision.revisionNumber}`;
  }

  protected updateNewImage(event: Event): void {
    this.newImage.set((event.target as HTMLInputElement).value);
  }

  private load(id: string): void {
    this.loading.set(true);
    this.actionError.set(null);
    this.service.getById(id).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (detail) => {
        this.deployment.set(detail);
        this.newImage.set(detail.image);
        this.service.getLogs(id).subscribe({
          next: (value) => this.logs.set(value),
          error: () => this.logs.set(''),
        });
      },
      error: (error) => {
        this.actionError.set(this.auth.errorMessage(error));
        this.deployment.set(null);
      },
    });
  }

  private runAction(operation: import('rxjs').Observable<unknown>, onSuccess?: () => void): void {
    if (this.actionInProgress()) {
      return;
    }

    this.actionError.set(null);
    this.actionInProgress.set(true);
    operation.pipe(finalize(() => this.actionInProgress.set(false))).subscribe({
      next: () => onSuccess?.(),
      error: (error) => this.actionError.set(this.auth.errorMessage(error)),
    });
  }

  private buildRequest(current: DeploymentDetail, overrides: Partial<Pick<DeploymentFormValue, 'image'>>): DeploymentFormValue {
    return {
      projectId: current.projectId ?? '',
      projectName: current.projectName,
      name: current.name,
      namespace: current.namespace,
      replicas: current.replicas,
      image: overrides.image ?? current.image,
      port: current.port,
      cpu: current.cpu,
      memory: current.memory,
      envVariables: current.configVariables ?? current.envVariables ?? {},
      secretVariables: current.secretVariables ?? {},
      // gitRepository: current.gitRepository ?? '',
      // gitBranch: current.gitBranch ?? '',
      // gitCommit: current.gitCommit ?? '',
      // gitTag: current.gitTag ?? '',
      // requestedHostname: current.requestedHostname ?? '',
      // requestedPath: current.requestedPath ?? '',
      // tlsEnabled: current.tlsEnabled ?? false,
      // tlsSecretName: current.tlsSecretName ?? '',
    };
  }
}
