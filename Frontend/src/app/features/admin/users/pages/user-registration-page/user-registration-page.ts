import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { finalize } from 'rxjs';

import { AuthService } from '../../../../../core/auth/services/auth.service';
import { CurrentUser } from '../../../../../core/auth/models/auth.models';
import { CreateUserRequest } from '../../../../../core/users/models/admin-user.models';
import { AdminUserService } from '../../../../../core/users/services/admin-user.service';
import { UserRegistrationForm } from '../../components/user-registration-form/user-registration-form';

@Component({
  selector: 'app-user-registration-page',
  imports: [UserRegistrationForm],
  templateUrl: './user-registration-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserRegistrationPage {
  private readonly adminUsers = inject(AdminUserService);
  private readonly auth = inject(AuthService);

  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly createdUser = signal<CurrentUser | null>(null);

  protected register(request: CreateUserRequest): void {
    if (this.loading()) return;

    this.loading.set(true);
    this.errorMessage.set(null);
    this.createdUser.set(null);

    this.adminUsers
      .createUser(request)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (user) => this.createdUser.set(user),
        error: (error) => this.errorMessage.set(this.auth.errorMessage(error)),
      });
  }
}
