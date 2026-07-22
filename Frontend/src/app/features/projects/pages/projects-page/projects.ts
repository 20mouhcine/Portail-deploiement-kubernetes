import { ChangeDetectionStrategy, Component, inject, OnInit, signal, computed } from '@angular/core';
import { ProjectsService } from '../../../../core/projects/services/projects.service';
import { AuthService } from '../../../../core/auth/services/auth.service';
import { AppSidebar } from '../../../../layout/app-sidebar/app-sidebar';
import { finalize } from 'rxjs';
import { Router } from '@angular/router';
import Project, { ProjectFormValue } from '../../../../core/projects/models/projects.model';
import { CreateProjectsFormModal } from '../../components/create-projects-form-modal/create-projects-form-modal';
import { ActionsPopover } from '../../components/actions-popover/actions-popover';
import { ConfirmDeleteModal } from '../../components/confirm-delete-modal/confirm-delete-modal';

@Component({
  selector: 'app-apps',
  standalone: true,
  templateUrl: './projects.html',
  imports: [AppSidebar, CreateProjectsFormModal, ActionsPopover, ConfirmDeleteModal],
  changeDetection: ChangeDetectionStrategy.OnPush,

})
export class Projects implements OnInit {
  private service = inject(ProjectsService);
  private auth = inject(AuthService);
  protected readonly user = this.auth.user;
  protected readonly loggingOut = signal(false);
  private readonly router = inject(Router);


  protected readonly projects = signal<Project[]>([]);
  protected readonly searchTerm = signal('');
  protected readonly viewMode = signal<'cards' | 'list'>('cards');
  protected readonly isAdmin = computed(() => this.user()?.roles.includes('ADMIN') ?? false);
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
        owner_id: project.ownerId ?? this.user()!.id
      };
    }
    return {
      owner_id: this.user()!.id,
      name: '',
      description: '',
      repository: ''
    };
  });

  protected logout(): void {
    if (this.loggingOut()) {
      return;
    }

    this.loggingOut.set(true);
    this.auth
      .logout()
      .pipe(finalize(() => this.loggingOut.set(false)))
      .subscribe({
        next: () => void this.router.navigate(['/login']),
        error: () => void this.router.navigate(['/login']),
      });
  }

  ngOnInit() {
    this.refresh();
  }

  refresh() {
    this.service.loadProjects().subscribe(projects => this.projects.set(projects));

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
    console.log('[projects.ts] saveProject called with:', project);
    console.log('[projects.ts] current modalMode:', this.modalMode());

    if (this.modalMode() === 'create') {
      console.log('[projects.ts] Sending create request');
      this.service.create(project).subscribe({
        next: () => {
          this.showForm.set(false);
          this.refresh();
        },
        error: (err) => console.error('Create error:', err)
      });
    } else {
      const current = this.editingProject();
      console.log('[projects.ts] current editingProject:', current);

      if (current) {
        console.log('[projects.ts] Calling this.service.update for id:', current.id);
        this.service.update({ ...project, id: current.id }).subscribe({
          next: () => {
            console.log('[projects.ts] Update request succeeded!');
            this.showForm.set(false);
            this.editingProject.set(null);
            this.refresh();
          },
          error: (err) => console.error('[projects.ts] Update error:', err)
        });
      } else {
        console.error('[projects.ts] ERROR: modalMode is edit, but editingProject is null!');
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
      this.service.delete(project.id).subscribe({
        next: () => {
          this.showConfirmDelete.set(false);
          this.projectToDelete.set(null);
          this.refresh();
        },
        error: (err) => {
          console.error('Delete error:', err);
          this.showConfirmDelete.set(false);
          this.projectToDelete.set(null);
        }
      });
    }
  }
}
