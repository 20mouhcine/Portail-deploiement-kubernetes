import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { DeploymentStatus } from '../../../../core/deployments/models/deployment.models';

@Component({
  selector: 'app-deployment-status-badge',
  templateUrl: './deployment-status-badge.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DeploymentStatusBadge {
  readonly status = input.required<DeploymentStatus>();

  protected readonly label = computed(() => ({
    PENDING: 'En attente', RUNNING: 'En cours', FAILED: 'Échec',
    SUCCEEDED: 'Opérationnel', STOPPED: 'Arrêté',
  })[this.status()]);

  protected readonly classes = computed(() => ({
    PENDING: 'bg-amber-50 text-amber-700 ring-amber-600/15',
    RUNNING: 'bg-blue-50 text-blue-700 ring-blue-600/15',
    FAILED: 'bg-red-50 text-red-700 ring-red-600/15',
    SUCCEEDED: 'bg-emerald-50 text-emerald-700 ring-emerald-600/15',
    STOPPED: 'bg-slate-100 text-slate-600 ring-slate-500/15',
  })[this.status()]);
}
