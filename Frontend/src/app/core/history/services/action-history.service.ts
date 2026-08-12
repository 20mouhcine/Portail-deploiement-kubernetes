import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';

import { AuthService } from '../../auth/services/auth.service';
import { ActionHistory } from '../models/action-history.models';

@Injectable({ providedIn: 'root' })
export class ActionHistoryService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly historyState = signal<readonly ActionHistory[]>([]);

  readonly entries = this.historyState.asReadonly();
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.http.get<ActionHistory[]>('/api/action-history').subscribe({
      next: (entries) => {
        this.historyState.set(entries);
        this.loading.set(false);
      },
      error: (error) => {
        this.historyState.set([]);
        this.error.set(this.auth.errorMessage(error));
        this.loading.set(false);
      },
    });
  }
}
