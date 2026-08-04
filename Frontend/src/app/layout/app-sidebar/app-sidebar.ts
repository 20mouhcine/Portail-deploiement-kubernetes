import { ChangeDetectionStrategy, Component, input, output, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { CurrentUser } from '../../core/auth/models/auth.models';
import { BrandMark } from '../../shared/components/brand-mark/brand-mark';
import { RoleBadge } from '../../shared/components/role-badge/role-badge';

const COLLAPSE_STORAGE_KEY = 'app-sidebar-collapsed';

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive, BrandMark, RoleBadge],
  templateUrl: './app-sidebar.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppSidebar {
  readonly user = input.required<CurrentUser>();
  readonly loggingOut = input(false);
  readonly mobileOpen = input(false);
  readonly logoutRequested = output<void>();
  readonly mobileCloseRequested = output<void>();

  readonly collapsedChange = output<boolean>();

  protected readonly collapsed = signal(this.readStoredCollapsed());

  protected toggleCollapse(): void {
    const next = !this.collapsed();
    this.collapsed.set(next);
    try {
      localStorage.setItem(COLLAPSE_STORAGE_KEY, String(next));
    } catch {
      // localStorage unavailable (e.g. private browsing) â€” ignore, state still works in-memory
    }
    this.collapsedChange.emit(next);
  }

  protected closeMobileMenu(): void { this.mobileCloseRequested.emit(); }

  private readStoredCollapsed(): boolean {
    try {
      return localStorage.getItem(COLLAPSE_STORAGE_KEY) === 'true';
    } catch {
      return false;
    }
  }
}
