import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { RoleName } from '../../../../../core/auth/models/auth.models';
import { CreateUserRequest } from '../../../../../core/users/models/admin-user.models';

@Component({
  selector: 'app-user-registration-form',
  imports: [ReactiveFormsModule],
  templateUrl: './user-registration-form.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserRegistrationForm {
  readonly loading = input(false);
  readonly registrationSubmitted = output<CreateUserRequest>();

  protected readonly form = new FormGroup({
    username: new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(50),
        Validators.pattern(/^[a-zA-Z0-9._-]+$/),
      ],
    }),
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email, Validators.maxLength(150)],
    }),
    password: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(12), Validators.maxLength(128)],
    }),
    passwordConfirmation: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required],
    }),
    admin: new FormControl(false, { nonNullable: true }),
    devops: new FormControl(false, { nonNullable: true }),
    developer: new FormControl(true, { nonNullable: true }),
  });

  protected submit(): void {
    const value = this.form.getRawValue();
    const roles = this.selectedRoles(value);

    if (value.password !== value.passwordConfirmation) {
      this.form.controls.passwordConfirmation.setErrors({ mismatch: true });
    }

    if (roles.length === 0) {
      this.form.setErrors({ roleRequired: true });
    } else if (this.form.hasError('roleRequired')) {
      this.form.setErrors(null);
    }

    if (this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }

    this.registrationSubmitted.emit({
      username: value.username.trim(),
      email: value.email.trim(),
      password: value.password,
      roles,
    });
  }

  private selectedRoles(value: ReturnType<typeof this.form.getRawValue>): RoleName[] {
    const roles: RoleName[] = [];
    if (value.admin) roles.push('ADMIN');
    if (value.devops) roles.push('DEVOPS');
    if (value.developer) roles.push('DEVELOPER');
    return roles;
  }
}
