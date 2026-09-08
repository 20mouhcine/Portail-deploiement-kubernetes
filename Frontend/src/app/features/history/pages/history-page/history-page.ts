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
  protected readonly loading = this.historyService.loading;
  protected readonly loadError = this.historyService.error;
  protected readonly searchTerm = signal('');
  protected readonly actionFilter = signal<ActionType | 'ALL'>('ALL');
  protected readonly dateFilter = signal<string>(this.getTodayString());

  protected readonly changeCount = computed(() => this.filteredEntries().filter((entry) => ['CREATE', 'UPDATE', 'SCALE', 'RESTART'].includes(entry.action)).length);
  protected readonly securityCount = computed(() => this.filteredEntries().filter((entry) => entry.action === 'LOGIN' || entry.action === 'LOGOUT').length);
  protected readonly deleteCount = computed(() => this.filteredEntries().filter((entry) => entry.action === 'DELETE').length);
  protected readonly isTodaySelected = computed(() => this.dateFilter() === this.getTodayString());

  protected readonly filteredEntries = computed(() => {
    const term = this.searchTerm().trim().toLocaleLowerCase('fr');
    const action = this.actionFilter();
    const selectedDate = this.dateFilter();

    return this.entries().filter((entry) => {
      const matchesAction = action === 'ALL' || entry.action === action;
      const matchesSearch = !term || [entry.details, entry.username, entry.targetName, entry.ipAddress]
        .some((value) => value ? value.toLocaleLowerCase('fr').includes(term) : false);

      let matchesDate = true;
      if (selectedDate) {
        const d = new Date(entry.createdAt);
        if (!isNaN(d.getTime())) {
          const year = d.getFullYear();
          const month = String(d.getMonth() + 1).padStart(2, '0');
          const day = String(d.getDate()).padStart(2, '0');
          const entryDateStr = `${year}-${month}-${day}`;
          matchesDate = entryDateStr === selectedDate;
        }
      }

      return matchesAction && matchesSearch && matchesDate;
    });
  });

  constructor() {
    this.historyService.load();
  }

  private getTodayString(): string {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  protected reload(): void { this.historyService.load(); }
  protected search(event: Event): void { this.searchTerm.set((event.target as HTMLInputElement).value); }
  protected filterByAction(event: Event): void { this.actionFilter.set((event.target as HTMLSelectElement).value as ActionType | 'ALL'); }
  protected filterByDate(event: Event): void { this.dateFilter.set((event.target as HTMLInputElement).value); }
  protected clearDateFilter(): void { this.dateFilter.set(''); }
  protected setTodayFilter(): void { this.dateFilter.set(this.getTodayString()); }
}
