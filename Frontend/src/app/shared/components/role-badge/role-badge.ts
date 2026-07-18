import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { RoleName } from '../../../core/auth/models/auth.models';

@Component({
  selector: 'app-role-badge',
  templateUrl: './role-badge.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RoleBadge {
  readonly role = input.required<RoleName>();

  protected readonly label = computed(() => {
    const labels: Record<RoleName, string> = {
      ADMIN: 'Administrateur',
      DEVOPS: 'DevOps',
      DEVELOPER: 'Développeur',
    };
    return labels[this.role()];
  });

  protected readonly classes = computed(() => {
    const styles: Record<RoleName, string> = {
      ADMIN: 'bg-violet-50 text-violet-700 ring-violet-600/15',
      DEVOPS: 'bg-blue-50 text-blue-700 ring-blue-600/15',
      DEVELOPER: 'bg-emerald-50 text-emerald-700 ring-emerald-600/15',
    };
    return styles[this.role()];
  });
}
