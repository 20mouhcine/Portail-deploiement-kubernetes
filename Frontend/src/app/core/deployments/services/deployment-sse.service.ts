import { Injectable, NgZone, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { DeploymentEvent, DeploymentStatusChange } from '../models/deployment-event.model';

export type SseConnectionState = 'connecting' | 'connected' | 'reconnecting' | 'closed';

@Injectable({ providedIn: 'root' })
export class DeploymentSseService {
  private readonly API_URL = '/api';
  readonly connectionState = signal<SseConnectionState>('closed');

  constructor(private zone: NgZone) {}

  subscribeToDeploymentLogs(id: string): Observable<DeploymentEvent> {
    return this.connect(`${this.API_URL}/deployments/${id}/events`, 'log', value => value as DeploymentEvent);
  }

  subscribeToProjectDeployments(id: string): Observable<DeploymentStatusChange> {
    return this.connect(`${this.API_URL}/projects/${id}/deployments/stream`, 'status_change', value => value as DeploymentStatusChange);
  }

  subscribeToAllDeployments(): Observable<DeploymentStatusChange> {
    return this.connect(`${this.API_URL}/deployments/stream`, 'status_change', value => value as DeploymentStatusChange);
  }

  private connect<T>(url: string, eventName: string, deserialize: (value: unknown) => T): Observable<T> {
    return new Observable<T>(observer => {
      let source: EventSource | null = null;
      let timer: ReturnType<typeof setTimeout> | null = null;
      let delay = 1000;
      let stopped = false;
      const state = (value: SseConnectionState) => this.zone.run(() => this.connectionState.set(value));
      const open = () => {
        if (stopped) return;
        state(delay === 1000 ? 'connecting' : 'reconnecting');
        source = new EventSource(url, { withCredentials: true });
        source.onopen = () => { delay = 1000; state('connected'); };
        source.addEventListener(eventName, (event: MessageEvent) => {
          try {
            this.zone.run(() => observer.next(deserialize(JSON.parse(event.data) as unknown)));
          } catch { /* Ignore malformed events, keep the stream alive. */ }
        });
        source.onerror = () => {
          source?.close();
          source = null;
          if (stopped || timer) return;
          state('reconnecting');
          timer = setTimeout(() => { timer = null; open(); }, delay);
          delay = Math.min(delay * 2, 30000);
        };
      };
      open();
      return () => {
        stopped = true;
        source?.close();
        if (timer) clearTimeout(timer);
        state('closed');
      };
    });
  }
}
