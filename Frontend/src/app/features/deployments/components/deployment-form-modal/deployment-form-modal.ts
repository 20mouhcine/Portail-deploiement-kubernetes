import { ChangeDetectionStrategy, Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { DeploymentFormValue } from '../../../../core/deployments/models/deployment.models';
import { ProjectsService } from '../../../../core/projects/services/projects.service';
import Project from '../../../../core/projects/models/projects.model';

const EMPTY_DEPLOYMENT: DeploymentFormValue = {
  projectId: '', projectName: '', name: '', namespace: 'default', replicas: 1,
  image: '', port: 8080, cpu: '250m', memory: '256Mi', envVariables: {},
  secretVariables: {}
};

// Which controls belong to which step — used to validate before advancing
const STEP_CONTROLS: readonly (readonly string[])[] = [
  ['projectId', 'projectName', 'name', 'namespace'],
  ['image', 'replicas', 'port', 'cpu', 'memory'],
  ['secretVariables'], // envVariables FormArray has no required validators, nothing to block on
];

const STEP_LABELS = ['Projet', 'Conteneur','Variables & secrets'];

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

  protected isProjectSelected = signal(false);
  protected readonly currentStep = signal(0);
  protected readonly stepLabels = STEP_LABELS;
  protected readonly totalSteps = STEP_LABELS.length;
  protected readonly isLastStep = computed(() => this.currentStep() === this.totalSteps - 1);
  protected readonly isFirstStep = computed(() => this.currentStep() === 0);

  protected readonly envVariables = new FormArray<FormGroup>([]);

  protected readonly form = new FormGroup({
    projectId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    projectName: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    namespace: new FormControl('default', { nonNullable: true, validators: [Validators.required] }),
    image: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    replicas: new FormControl(1, { nonNullable: true, validators: [Validators.required, Validators.min(1)] }),
    port: new FormControl(8080, { nonNullable: true, validators: [Validators.required, Validators.min(1), Validators.max(65535)] }),
    cpu: new FormControl('250m', { nonNullable: true, validators: [Validators.required] }),
    memory: new FormControl('256Mi', { nonNullable: true, validators: [Validators.required] }),
    // gitRepository: new FormControl('', { nonNullable: true }),
    // gitBranch: new FormControl('', { nonNullable: true }),
    // gitCommit: new FormControl('', { nonNullable: true }),
    // gitTag: new FormControl('', { nonNullable: true }),
    // requestedHostname: new FormControl('', { nonNullable: true }),
    // requestedPath: new FormControl('', { nonNullable: true }),
    // tlsEnabled: new FormControl(false, { nonNullable: true }),
    // tlsSecretName: new FormControl('', { nonNullable: true }),
    secretVariables: new FormControl('{}', { nonNullable: true }),
    envVariables: this.envVariables,
  });

  allProjects: Project[] = [];
  filteredProjects: Project[] = [];
  projectsService = inject(ProjectsService);

  constructor() {
    effect(() => {
      if (this.open()) {
        this.currentStep.set(0);
        const v = this.value();
        this.form.patchValue({
          projectId: v.projectId,
          projectName: v.projectName,
          name: v.name,
          namespace: v.namespace,
          image: v.image,
          replicas: v.replicas,
          port: v.port,
          cpu: v.cpu,
          memory: v.memory,
          // gitRepository: v.gitRepository,
          // gitBranch: v.gitBranch,
          // gitCommit: v.gitCommit,
          // gitTag: v.gitTag,
          // requestedHostname: v.requestedHostname,
          // requestedPath: v.requestedPath,
          // tlsEnabled: v.tlsEnabled,
          // tlsSecretName: v.tlsSecretName,
          secretVariables: JSON.stringify(v.secretVariables ?? {}, null, 2),
        });
        const immutableControls = ['projectId', 'projectName', 'name', 'namespace'] as const;
        immutableControls.forEach((control) => this.mode() === 'edit'
          ? this.form.controls[control].disable()
          : this.form.controls[control].enable());
        this.envVariables.clear();
        Object.entries(v.envVariables ?? {}).forEach(([key, value]) => {
          this.envVariables.push(this.buildEnvRow(key, value));
        });
      }
    });
    this.projectsService.loadProjects().subscribe((projects) => {
      this.allProjects = projects;
    });
  }

  protected goToStep(index: number): void {
    // Only allow jumping directly to a step you've already validated up to
    if (index <= this.currentStep() || this.isStepValid(this.currentStep())) {
      this.currentStep.set(index);
    }
  }

  protected nextStep(): void {
    if (!this.isStepValid(this.currentStep())) {
      this.markStepTouched(this.currentStep());
      return;
    }
    if (!this.isLastStep()) {
      this.currentStep.update((s) => s + 1);
    }
  }

  protected prevStep(): void {
    if (!this.isFirstStep()) {
      this.currentStep.update((s) => s - 1);
    }
  }

  private isStepValid(step: number): boolean {
    return STEP_CONTROLS[step].every((name) =>{
      const control = this.form.get(name);
      if(!control || control.disabled) {
        return true;
      }
      return control.valid;
    });
  }

 private markStepTouched(step: number): void {
  STEP_CONTROLS[step].forEach((name) => {
    const control = this.form.get(name);
    if (control && control.enabled) control.markAsTouched();
  });
}

  protected addEnvVar(): void {
    this.envVariables.push(this.buildEnvRow('', ''));
  }

  protected removeEnvVar(index: number): void {
    this.envVariables.removeAt(index);
  }

  private buildEnvRow(key: string, value: string): FormGroup {
    return new FormGroup({
      key: new FormControl(key, { nonNullable: true }),
      value: new FormControl(value, { nonNullable: true }),
    });
  }

  selectProject(project: Project) {
    this.form.patchValue({ projectId: project.id, projectName: project.name });
    this.toggleProject();
  }

  toggleProject() {
    this.isProjectSelected.set(!this.isProjectSelected());
  }

  onUserTyping(event: Event) {
    const target = event.target as HTMLInputElement;
    this.filteredProjects = this.allProjects.filter((project) =>
      project.name.toLowerCase().includes(target.value.toLowerCase())
    );
    this.toggleProject();
  }

  protected close(): void { this.closed.emit(); }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      // Jump back to the first invalid step so the user sees what's wrong
      const firstInvalidStep = STEP_CONTROLS.findIndex((_, i) => !this.isStepValid(i));
      if (firstInvalidStep !== -1) this.currentStep.set(firstInvalidStep);
      return;
    }
    const raw = this.form.getRawValue();
    let secretVariables: Record<string, string> = {};
    try {
      secretVariables = raw.secretVariables ? JSON.parse(raw.secretVariables) as Record<string, string> : {};
    } catch {
      this.form.controls.secretVariables.setErrors({ invalidJson: true });
      this.currentStep.set(4);
      return;
    }
    const envVariables: Record<string, string> = Object.fromEntries(
      this.envVariables.controls.map(group => [
        group.get('key')!.value as string,
        group.get('value')!.value as string,
      ])
    );

    const value: DeploymentFormValue = {
      ...raw,
      envVariables,
      secretVariables,
    };

    this.submitted.emit(value);
  }
}