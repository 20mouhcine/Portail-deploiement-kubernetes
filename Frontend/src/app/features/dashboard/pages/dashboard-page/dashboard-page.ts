import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { AuthService } from '../../../../core/auth/services/auth.service';
import { StatusCard } from '../../../../shared/components/status-card/status-card';

@Component({
  selector: 'app-dashboard-page',
  imports: [StatusCard],
  templateUrl: './dashboard-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardPage {
  private readonly auth = inject(AuthService);
  protected readonly user = this.auth.user;
}
