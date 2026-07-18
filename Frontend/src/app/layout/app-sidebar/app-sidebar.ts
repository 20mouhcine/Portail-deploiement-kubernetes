import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { CurrentUser } from '../../core/auth/models/auth.models';
import { BrandMark } from '../../shared/components/brand-mark/brand-mark';
import { RoleBadge } from '../../shared/components/role-badge/role-badge';

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive, BrandMark, RoleBadge],
  templateUrl: './app-sidebar.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppSidebar {
  readonly user = input.required<CurrentUser>();
  readonly loggingOut = input(false);
  readonly logoutRequested = output<void>();
}
