import { ChangeDetectionStrategy, Component, inject, OnInit, signal, computed } from '@angular/core';
import { ProjectsService } from '../../../../core/projects/services/projects.service';
import { AuthService } from '../../../../core/auth/services/auth.service';
import Project, { ProjectFormValue } from '../../../../core/projects/models/projects.model';
import { CreateProjectsFormModal } from '../../components/create-projects-form-modal/create-projects-form-modal';
import { ActionsPopover } from '../../components/actions-popover/actions-popover';
import { ConfirmDeleteModal } from '../../components/confirm-delete-modal/confirm-delete-modal';
import { finalize, Observable } from 'rxjs';

@Component({
  selector: 'app-apps',
  standalone: true,
  templateUrl: './projects.html',
  imports: [CreateProjectsFormModal, ActionsPopover, ConfirmDeleteModal],
  changeDetection: ChangeDetectionStrategy.OnPush,

})
export class Projects implements OnInit {
  private service = inject(ProjectsService);
  private auth = inject(AuthService);
  protected readonly user = this.auth.user;




  protected readonly projects = signal<Project[]>([]);
  protected readonly loading = signal(true);
  protected readonly actionInProgress = signal(false);
  protected readonly actionError = signal<string | null>(null);
  protected readonly searchTerm = signal('');
  protected readonly viewMode = signal<'cards' | 'list'>('cards');
  protected readonly hasAccess = computed(() => (this.user()?.roles.includes('ADMIN') || this.user()?.roles.includes("DEVELOPER")) ?? false);
  protected readonly filteredProjects = computed(() => {
    const term = this.searchTerm().trim().toLocaleLowerCase('fr');
    if (!term) {
      return this.projects();
    }

    return this.projects().filter((project) =>
      project.name.toLocaleLowerCase('fr').includes(term)
      || (project.ownerUsername ?? '').toLocaleLowerCase('fr').includes(term)
    );
  });
  protected readonly showForm = signal(false);
  protected readonly modalMode = signal<'create' | 'edit'>('create');
  protected readonly editingProject = signal<Project | null>(null);

  protected readonly showConfirmDelete = signal(false);
  protected readonly projectToDelete = signal<Project | null>(null);

  protected readonly modalValue = computed<ProjectFormValue>(() => {
    const project = this.editingProject();
    if (project) {
      return {
        name: project.name,
        description: project.description || '',
        repository: project.repository,
        owner_id: project.ownerId ?? this.user()?.id ?? '',
        allowedNamespaces: project.allowedNamespaces ?? ['default'],
        allowedUsers: project.allowedUsers ?? [],
      };
    }
    return {
      owner_id: this.user()?.id ?? '',
      name: '',
      description: '',
      repository: '',
      allowedNamespaces: ['default'],
      allowedUsers: [],
    };
  });

  ngOnInit() {
    this.refresh();
  }

  refresh() {
    this.loading.set(true);
    this.actionError.set(null);
    this.service.loadProjects().pipe(finalize(() => this.loading.set(false))).subscribe({
      next: projects => this.projects.set(projects),
      error: error => {
        this.projects.set([]);
        this.actionError.set(this.auth.errorMessage(error));
      },
    });

  }

  searchProjects(event: Event): void {
    this.searchTerm.set((event.target as HTMLInputElement).value);
  }

  setViewMode(mode: 'cards' | 'list'): void {
    this.viewMode.set(mode);
  }


  cancel() {
    this.showForm.set(false);
    this.editingProject.set(null);
  }

  saveProject(project: ProjectFormValue) {
    if (this.modalMode() === 'create') {
      this.runAction(this.service.create(project), () => this.cancel());
    } else {
      const current = this.editingProject();
      if (current) {
        this.runAction(this.service.update({ ...project, id: current.id }), () => this.cancel());
      }
    }
  }

  openModal() {
    this.modalMode.set('create');
    this.editingProject.set(null);
    this.showForm.set(true);
  }

  handleEdit(project: Project): void {
    this.modalMode.set('edit');
    this.editingProject.set(project);
    this.showForm.set(true);
  }

  handleDelete(project: Project): void {
    this.projectToDelete.set(project);
    this.showConfirmDelete.set(true);
  }

  cancelDelete(): void {
    this.showConfirmDelete.set(false);
    this.projectToDelete.set(null);
  }

  confirmDelete(): void {
    const project = this.projectToDelete();
    if (project) {
      this.runAction(this.service.delete(project.id), () => this.cancelDelete());
    }
  }

  private runAction(operation: Observable<unknown>, onSuccess: () => void): void {
    if (this.actionInProgress()) return;
    this.actionError.set(null);
    this.actionInProgress.set(true);
    operation.pipe(finalize(() => this.actionInProgress.set(false))).subscribe({
      next: () => {
        onSuccess();
        this.refresh();
      },
      error: error => this.actionError.set(this.auth.errorMessage(error)),
    });
  }
}

