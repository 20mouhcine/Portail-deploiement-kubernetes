import { ChangeDetectionStrategy, Component, input, output, signal } from '@angular/core';

import { CurrentUser, RoleName } from '../../../../../core/auth/models/auth.models';
import { RoleBadge } from '../../../../../shared/components/role-badge/role-badge';

@Component({
  selector: 'app-user-management-list',
  imports: [RoleBadge],
  templateUrl: './user-management-list.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserManagementList {
  readonly users = input.required<readonly CurrentUser[]>();
  readonly currentUserId = input<string | null>(null);
  readonly loadingUserId = input<string | null>(null);

  readonly rolesChanged = output<{ user: CurrentUser; roles: readonly RoleName[] }>();
  readonly statusChanged = output<CurrentUser>();

  protected readonly viewMode = signal<'cards' | 'list'>('cards');
  protected readonly editingUser = signal<CurrentUser | null>(null);
  protected readonly selectedRoles = signal<readonly RoleName[]>([]);
  protected readonly availableRoles: readonly RoleName[] = ['ADMIN', 'DEVOPS', 'DEVELOPER'];

  protected setViewMode(mode: 'cards' | 'list'): void {
    this.viewMode.set(mode);
  }

  protected openEditor(user: CurrentUser): void {
    this.editingUser.set(user);
    this.selectedRoles.set([...user.roles]);
  }

  protected closeEditor(): void {
    this.editingUser.set(null);
    this.selectedRoles.set([]);
  }

  protected toggleRole(role: RoleName): void {
    this.selectedRoles.update((roles) =>
      roles.includes(role) ? roles.filter((current) => current !== role) : [...roles, role]
    );
  }

  protected saveRoles(): void {
    const user = this.editingUser();
    if (!user || this.selectedRoles().length === 0) return;

    this.rolesChanged.emit({ user, roles: this.selectedRoles() });
    this.closeEditor();
  }

  protected toggleStatus(user: CurrentUser): void {
    if (user.id !== this.currentUserId()) {
      this.statusChanged.emit(user);
    }
  }

  protected roleLabel(role: RoleName): string {
    return role === 'ADMIN' ? 'Administrateur' : role === 'DEVOPS' ? 'DevOps' : 'Développeur';
  }
}
