import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthService } from '../../core/auth/services/auth.service';
import { AppSidebar } from '../app-sidebar/app-sidebar';

@Component({
  selector: 'app-authenticated-layout',
  imports: [RouterOutlet, AppSidebar],
  templateUrl: './authenticated-layout.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuthenticatedLayout {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly user = this.auth.user;
  protected readonly loggingOut = signal(false);
  protected readonly mobileMenuOpen = signal(false);
  protected readonly sidebarCollapsed = signal(this.readStoredSidebarState());

  protected toggleMobileMenu(): void { this.mobileMenuOpen.update((open) => !open); }
  protected closeMobileMenu(): void { this.mobileMenuOpen.set(false); }

  private readStoredSidebarState(): boolean {
    try { return localStorage.getItem('app-sidebar-collapsed') === 'true'; } catch { return false; }
  }

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

