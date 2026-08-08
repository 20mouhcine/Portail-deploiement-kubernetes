import { ChangeDetectionStrategy, Component, effect, input, output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { ProjectFormValue } from '../../../../core/projects/models/projects.model';
import { AuthService } from '../../../../core/auth/services/auth.service';
import { inject } from '@angular/core';

@Component({
  selector: 'app-create-projects-form-modal',
  imports: [ReactiveFormsModule],
  templateUrl: './create-projects-form-modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreateProjectsFormModal {
  readonly open = input(false);
  readonly mode = input<'create' | 'edit'>('create');
  readonly userId = inject(AuthService).user()?.id ?? '';
  readonly value = input<ProjectFormValue>({
    owner_id: this.userId,
    name: '',
    description: '',
    repository: '',
    allowedNamespaces: ['default'],
    allowedUsers: [],
  });


  readonly closed = output<void>();
  readonly submitted = output<ProjectFormValue>();

  protected readonly form = new FormGroup({
    name: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required],
    }),
    description: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required],
    }),
    repository: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required],
    }),
    owner_id: new FormControl(this.userId, {
      nonNullable: true,
      validators: [Validators.required],
    }),
    allowedNamespaces: new FormControl('default', { nonNullable: true, validators: [Validators.required] }),
    allowedUsers: new FormControl('', { nonNullable: true }),
  });

  constructor() {
    effect(() => {
      if (!this.open()) {
        return;
      }

      const value = this.value();
      this.form.reset({
        ...value,
        allowedNamespaces: (value.allowedNamespaces ?? ['default']).join(', '),
        allowedUsers: (value.allowedUsers ?? []).join(', '),
      });
    });
  }

  protected close(): void {
    this.closed.emit();
  }

  protected submit(): void {
    if (this.form.invalid) {
      console.warn("Form is invalid! Check required fields.", this.form.value);
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    this.submitted.emit({
      ...raw,
      allowedNamespaces: this.toList(raw.allowedNamespaces),
      allowedUsers: this.toList(raw.allowedUsers),
    });
  }

  private toList(value: string): string[] {
    return [...new Set(value.split(',').map(item => item.trim()).filter(Boolean))];
  }
}
