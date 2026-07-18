import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthService } from '../../../../core/auth/services/auth.service';
import { AppSidebar } from '../../../../layout/app-sidebar/app-sidebar';
import { StatusCard } from '../../../../shared/components/status-card/status-card';

@Component({
  selector: 'app-dashboard-page',
  imports: [AppSidebar, StatusCard],
  templateUrl: './dashboard-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly user = this.auth.user;
  protected readonly loggingOut = signal(false);

  protected logout(): void {
    if (this.loggingOut()) {
      return;
    }

    this.loggingOut.set(true);
    this.auth
      .logout()
      .pipe(finalize(() => this.loggingOut.set(false)))
      .subscribe({
        next: () => void this.router.navigate(['/login']),
        error: () => void this.router.navigate(['/login']),
      });
  }
}
