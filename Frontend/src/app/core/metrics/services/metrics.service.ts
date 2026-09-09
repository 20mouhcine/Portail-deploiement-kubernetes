import { Injectable, NgZone, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';

import { ClusterMetrics } from '../models/cluster-metrics.model';

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

@Injectable({ providedIn: 'root' })
export class MetricsService {
  private readonly http = inject(HttpClient);
  private readonly zone = inject(NgZone);
  private readonly apiUrl = '/api/metrics';

  readonly metrics = signal<ClusterMetrics | null>(null);
  readonly error = signal<string | null>(null);
  readonly connected = signal(false);

  private eventSource: EventSource | null = null;

  /** One-shot fetch for initial data */
  getClusterMetrics(): Observable<ClusterMetrics> {
    return this.http.get<ApiResponse<ClusterMetrics>>(`${this.apiUrl}/cluster`).pipe(
      map(response => response.data),
    );
  }

  /** Subscribe to the SSE stream and update the metrics signal in real time */
  startStreaming(): void {
    this.stopStreaming();
    this.error.set(null);

    // Fetch initial snapshot via REST API so UI updates immediately
    this.getClusterMetrics().subscribe({
      next: (data) => {
        this.metrics.set(data);
        this.connected.set(true);
        this.error.set(null);
      },
      error: (err) => {
        if (!this.metrics()) {
          this.error.set('Impossible de charger les métriques du cluster.');
        }
      }
    });

    const url = `${this.apiUrl}/stream`;
    this.eventSource = new EventSource(url, { withCredentials: true });

    this.eventSource.addEventListener('metrics', (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data) as ClusterMetrics;
        this.zone.run(() => {
          this.metrics.set(data);
          this.connected.set(true);
          this.error.set(null);
        });
      } catch {
        // Ignore malformed events
      }
    });

    this.eventSource.onopen = () => {
      this.zone.run(() => this.connected.set(true));
    };

    this.eventSource.onerror = () => {
      this.zone.run(() => {
        if (!this.metrics()) {
          this.connected.set(false);
          this.error.set('Connexion aux métriques perdue. Tentative de reconnexion...');
        }
      });
    };
  }

  stopStreaming(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
      this.connected.set(false);
    }
  }
}

