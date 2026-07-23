import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';

import { ActionType } from '../../../../core/history/models/action-history.models';
import { ActionHistoryService } from '../../../../core/history/services/action-history.service';
import { StatusCard } from '../../../../shared/components/status-card/status-card';
import { ActionTypeBadge } from '../../components/action-type-badge/action-type-badge';
import { HistoryEntry } from '../../components/history-entry/history-entry';

@Component({
  selector: 'app-history-page',
  imports: [DatePipe, StatusCard, ActionTypeBadge, HistoryEntry],
  templateUrl: './history-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HistoryPage {
  private readonly historyService = inject(ActionHistoryService);

  protected readonly entries = this.historyService.entries;
  protected readonly searchTerm = signal('');
  protected readonly actionFilter = signal<ActionType | 'ALL'>('ALL');
  protected readonly viewMode = signal<'timeline' | 'table'>('timeline');

  protected readonly changeCount = computed(() => this.entries().filter((entry) => ['CREATE', 'UPDATE', 'SCALE', 'RESTART'].includes(entry.action)).length);
  protected readonly securityCount = computed(() => this.entries().filter((entry) => entry.action === 'LOGIN' || entry.action === 'LOGOUT').length);
  protected readonly deleteCount = computed(() => this.entries().filter((entry) => entry.action === 'DELETE').length);
  protected readonly filteredEntries = computed(() => {
    const term = this.searchTerm().trim().toLocaleLowerCase('fr');
    const action = this.actionFilter();
    return this.entries().filter((entry) => {
      const matchesAction = action === 'ALL' || entry.action === action;
      const matchesSearch = !term || [entry.details, entry.username, entry.targetName, entry.ipAddress]
        .some((value) => value.toLocaleLowerCase('fr').includes(term));
      return matchesAction && matchesSearch;
    });
  });

  protected search(event: Event): void { this.searchTerm.set((event.target as HTMLInputElement).value); }
  protected filterByAction(event: Event): void { this.actionFilter.set((event.target as HTMLSelectElement).value as ActionType | 'ALL'); }
  protected setViewMode(mode: 'timeline' | 'table'): void { this.viewMode.set(mode); }
}
