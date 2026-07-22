import { ChangeDetectionStrategy, Component, effect, input, output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { DeploymentFormValue } from '../../../../core/deployments/models/deployment.models';

const EMPTY_DEPLOYMENT: DeploymentFormValue = {
  projectName: '', name: '', namespace: 'default', replicas: 1,
  image: '', port: 8080, cpu: '250m', memory: '256Mi',
};

@Component({
  selector: 'app-deployment-form-modal',
  imports: [ReactiveFormsModule],
  templateUrl: './deployment-form-modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DeploymentFormModal {
  readonly open = input(false);
  readonly mode = input<'create' | 'edit'>('create');
  readonly value = input<DeploymentFormValue>(EMPTY_DEPLOYMENT);
  readonly closed = output<void>();
  readonly submitted = output<DeploymentFormValue>();

  protected readonly form = new FormGroup({
    projectName: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    namespace: new FormControl('default', { nonNullable: true, validators: [Validators.required] }),
    image: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    replicas: new FormControl(1, { nonNullable: true, validators: [Validators.required, Validators.min(1)] }),
    port: new FormControl(8080, { nonNullable: true, validators: [Validators.required, Validators.min(1), Validators.max(65535)] }),
    cpu: new FormControl('250m', { nonNullable: true, validators: [Validators.required] }),
    memory: new FormControl('256Mi', { nonNullable: true, validators: [Validators.required] }),
  });

  constructor() {
    effect(() => { if (this.open()) this.form.reset(this.value()); });
  }

  protected close(): void { this.closed.emit(); }

  protected submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.submitted.emit(this.form.getRawValue());
  }
}
