import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { Deployment } from '../../../../core/deployments/models/deployment.models';
import { DeploymentStatusBadge } from '../deployment-status-badge/deployment-status-badge';
import { DeploymentsService } from '../../../../core/deployments/services/deployments.service';
import { inject } from '@angular/core';

@Component({
  selector: 'app-deployment-card',
  imports: [DeploymentStatusBadge],
  templateUrl: './deployment-card.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DeploymentCard {
  readonly deployment = input.required<Deployment>();
  readonly canManage = input(false);
  readonly operationInProgress = input(false);

  readonly editRequested = output<Deployment>();
  readonly detailsRequested = output<Deployment>();
  readonly restartRequested = output<Deployment>();
  readonly stopRequested = output<Deployment>();
  readonly scaleRequested = output<{ deployment: Deployment; replicas: number }>();
  readonly deleteRequested = output<Deployment>();
  readonly logsRequested = output<Deployment>();

  deploymentService = inject(DeploymentsService);


}
