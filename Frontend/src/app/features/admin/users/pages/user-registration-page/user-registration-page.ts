import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { finalize } from 'rxjs';

import { AuthService } from '../../../../../core/auth/services/auth.service';
import { CurrentUser, RoleName } from '../../../../../core/auth/models/auth.models';
import { CreateUserRequest } from '../../../../../core/users/models/admin-user.models';
import { AdminUserService } from '../../../../../core/users/services/admin-user.service';
import { UserRegistrationForm } from '../../components/user-registration-form/user-registration-form';
import { UserManagementList } from '../../components/user-management-list/user-management-list';

@Component({
  selector: 'app-user-registration-page',
  imports: [UserRegistrationForm, UserManagementList],
  templateUrl: './user-registration-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserRegistrationPage implements OnInit {
  private readonly adminUsers = inject(AdminUserService);
  private readonly auth = inject(AuthService);

  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly createdUser = signal<CurrentUser | null>(null);
  protected readonly currentUser = this.auth.user;
  protected readonly users = signal<readonly CurrentUser[]>([]);
  protected readonly usersLoading = signal(true);
  protected readonly loadingUserId = signal<string | null>(null);
  protected readonly managementError = signal<string | null>(null);

  ngOnInit(): void {
    this.refreshUsers();
  }

  protected register(request: CreateUserRequest): void {
    if (this.loading()) return;

    this.loading.set(true);
    this.errorMessage.set(null);
    this.createdUser.set(null);

    this.adminUsers
      .createUser(request)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (user) => {
          this.createdUser.set(user);
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
    this.usersLoading.set(true);
    this.managementError.set(null);
    this.adminUsers.getUsers()
      .pipe(finalize(() => this.usersLoading.set(false)))
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
