import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { CurrentUser } from '../../core/auth/models/auth.models';
import { BrandMark } from '../../shared/components/brand-mark/brand-mark';
import { RoleBadge } from '../../shared/components/role-badge/role-badge';

@Component({
  selector: 'app-header',
  imports: [BrandMark, RoleBadge],
  templateUrl: './app-header.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppHeader {
  readonly user = input.required<CurrentUser>();
  readonly logoutRequested = output<void>();
}
