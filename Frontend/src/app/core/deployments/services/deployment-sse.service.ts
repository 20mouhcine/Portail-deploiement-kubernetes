import { Injectable, NgZone } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { DeploymentEvent, DeploymentStatusChange } from '../models/deployment-event.model';

@Injectable({
  providedIn: 'root'
})
export class DeploymentSseService {

  private readonly API_URL = '/api';

  constructor(private zone: NgZone) {}

  subscribeToDeploymentLogs(deploymentId: string): Observable<DeploymentEvent> {
    return new Observable<DeploymentEvent>(observer => {
      const eventSource = new EventSource(`${this.API_URL}/deployments/${deploymentId}/events`);

      eventSource.addEventListener('log', (event: MessageEvent) => {
        this.zone.run(() => {
          observer.next(JSON.parse(event.data));
        });
      });

      eventSource.onerror = (error) => {
        this.zone.run(() => {
          // You might not want to error out immediately on SSE reconnect attempts
          // observer.error(error); 
        });
      };

      return () => {
        eventSource.close();
      };
    });
  }

  subscribeToProjectDeployments(projectId: string): Observable<DeploymentStatusChange> {
    return new Observable<DeploymentStatusChange>(observer => {
      const eventSource = new EventSource(`${this.API_URL}/projects/${projectId}/deployments/stream`);

      eventSource.addEventListener('status_change', (event: MessageEvent) => {
        this.zone.run(() => {
          observer.next(JSON.parse(event.data));
        });
      });

      eventSource.onerror = (error) => {
        this.zone.run(() => {
        });
      };

      return () => {
        eventSource.close();
      };
    });
  }

  subscribeToAllDeployments(): Observable<DeploymentStatusChange> {
    return new Observable<DeploymentStatusChange>(observer => {
      const eventSource = new EventSource(`${this.API_URL}/deployments/stream`);

      eventSource.addEventListener('status_change', (event: MessageEvent) => {
        this.zone.run(() => {
          observer.next(JSON.parse(event.data));
        });
      });

      eventSource.onerror = (error) => {
        this.zone.run(() => {
        });
      };

      return () => {
        eventSource.close();
      };
    });
  }
}
