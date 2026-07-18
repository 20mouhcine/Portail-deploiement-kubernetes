import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { CurrentUser } from '../../../../../core/auth/models/auth.models';
import { AuthService } from '../../../../../core/auth/services/auth.service';
import { AdminUserService } from '../../../../../core/users/services/admin-user.service';
import { RoleBadge } from '../../../../../shared/components/role-badge/role-badge';
import { StatusCard } from '../../../../../shared/components/status-card/status-card';

@Component({
  selector: 'app-admin-dashboard-page',
  imports: [RouterLink, RoleBadge, StatusCard],
  templateUrl: './admin-dashboard-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminDashboardPage implements OnInit {
  private readonly adminUsers = inject(AdminUserService);
  private readonly auth = inject(AuthService);

  protected readonly currentUser = this.auth.user;
  protected readonly users = signal<readonly CurrentUser[]>([]);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.adminUsers
      .getUsers()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (users) => this.users.set(users),
        error: (error) => this.errorMessage.set(this.auth.errorMessage(error)),
      });
  }

  protected activeUsers(): number {
    return this.users().filter((user) => user.enabled).length;
  }

  protected administrators(): number {
    return this.users().filter((user) => user.roles.includes('ADMIN')).length;
  }
}
