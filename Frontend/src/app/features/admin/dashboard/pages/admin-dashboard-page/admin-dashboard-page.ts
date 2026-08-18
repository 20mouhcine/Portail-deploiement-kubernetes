import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { finalize } from 'rxjs';

import { CurrentUser, RoleName } from '../../../../../core/auth/models/auth.models';
import { AuthService } from '../../../../../core/auth/services/auth.service';
import { CreateUserRequest } from '../../../../../core/users/models/admin-user.models';
import { AdminUserService } from '../../../../../core/users/services/admin-user.service';
import { StatusCard } from '../../../../../shared/components/status-card/status-card';
import { UserRegistrationForm } from '../../../users/components/user-registration-form/user-registration-form';
import { UserManagementList } from '../../../users/components/user-management-list/user-management-list';

@Component({
  selector: 'app-admin-dashboard-page',
  imports: [StatusCard, UserRegistrationForm, UserManagementList],
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

  protected readonly showRegistrationForm = signal(false);
  protected readonly creatingUser = signal(false);
  protected readonly createdUser = signal<CurrentUser | null>(null);
  
  protected readonly loadingUserId = signal<string | null>(null);
  protected readonly managementError = signal<string | null>(null);

  ngOnInit(): void {
    this.refreshUsers();
  }

  protected activeUsers(): number {
    return this.users().filter((user) => user.enabled).length;
  }

  protected administrators(): number {
    return this.users().filter((user) => user.roles.includes('ADMIN')).length;
  }

  protected toggleRegistrationForm(): void {
    this.showRegistrationForm.update((show) => !show);
    this.errorMessage.set(null);
    this.createdUser.set(null);
  }

  protected register(request: CreateUserRequest): void {
    if (this.creatingUser()) return;

    this.creatingUser.set(true);
    this.errorMessage.set(null);
    this.createdUser.set(null);

    this.adminUsers
      .createUser(request)
      .pipe(finalize(() => this.creatingUser.set(false)))
      .subscribe({
        next: (user) => {
          this.createdUser.set(user);
          this.showRegistrationForm.set(false);
          this.refreshUsers();
        },
        error: (error) => this.errorMessage.set(this.auth.errorMessage(error)),
      });
  }

  protected updateRoles(event: { user: CurrentUser; roles: readonly RoleName[] }): void {
    this.loadingUserId.set(event.user.id);
    this.managementError.set(null);
    this.adminUsers.updateRoles(event.user.id, { roles: event.roles })
      .pipe(finalize(() => this.loadingUserId.set(null)))
      .subscribe({
        next: (updatedUser) => this.replaceUser(updatedUser),
        error: (error) => this.managementError.set(this.auth.errorMessage(error)),
      });
  }

  protected toggleStatus(user: CurrentUser): void {
    this.loadingUserId.set(user.id);
    this.managementError.set(null);
    this.adminUsers.setEnabled(user.id, { enabled: !user.enabled })
      .pipe(finalize(() => this.loadingUserId.set(null)))
      .subscribe({
        next: (updatedUser) => this.replaceUser(updatedUser),
        error: (error) => this.managementError.set(this.auth.errorMessage(error)),
      });
  }

  private refreshUsers(): void {
    this.loading.set(true);
    this.managementError.set(null);
    this.adminUsers.getUsers()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (users) => this.users.set(users),
        error: (error) => this.managementError.set(this.auth.errorMessage(error)),
      });
  }

  private replaceUser(updatedUser: CurrentUser): void {
    this.users.update((users) =>
      users.map((user) => user.id === updatedUser.id ? updatedUser : user)
    );
  }
}
