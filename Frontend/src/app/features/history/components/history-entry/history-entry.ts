import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { ActionHistory } from '../../../../core/history/models/action-history.models';
import { ActionTypeBadge } from '../action-type-badge/action-type-badge';

@Component({
  selector: 'app-history-entry',
  imports: [DatePipe, ActionTypeBadge],
  templateUrl: './history-entry.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HistoryEntry {
  readonly entry = input.required<ActionHistory>();
  readonly last = input(false);

  protected readonly iconClasses = computed(() => ({
    CREATE: 'bg-emerald-50 text-emerald-600', UPDATE: 'bg-blue-50 text-blue-600',
    DELETE: 'bg-red-50 text-red-600', RESTART: 'bg-violet-50 text-violet-600',
    SCALE: 'bg-cyan-50 text-cyan-600', LOGIN: 'bg-slate-100 text-slate-600',
    LOGOUT: 'bg-amber-50 text-amber-600',
  })[this.entry().action]);
}
