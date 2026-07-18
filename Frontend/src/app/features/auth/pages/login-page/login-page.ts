import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs';

import { LoginCredentials } from '../../../../core/auth/models/auth.models';
import { AuthService } from '../../../../core/auth/services/auth.service';
import { BrandMark } from '../../../../shared/components/brand-mark/brand-mark';
import { LoginForm } from '../../components/login-form/login-form';

@Component({
  selector: 'app-login-page',
  imports: [BrandMark, LoginForm],
  templateUrl: './login-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected login(credentials: LoginCredentials): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.auth
      .login(credentials)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: () => {
          const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
          void this.router.navigateByUrl(
            returnUrl?.startsWith('/') ? returnUrl : '/dashboard',
          );
        },
        error: (error: unknown) => {
          this.errorMessage.set(this.auth.errorMessage(error));
        },
      });
  }
}
