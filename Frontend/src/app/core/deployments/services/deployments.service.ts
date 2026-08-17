import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { NEVER, Observable, Subject, catchError, finalize, map, of, shareReplay, startWith, switchMap, tap, timer, filter, take } from 'rxjs';

import { Deployment, DeploymentDetail, DeploymentFormValue, DeploymentStatus, DeploymentJobResponse } from '../models/deployment.models';
import { DeploymentSseService } from './deployment-sse.service';
import { AuthService } from '../../auth/services/auth.service';

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

@Injectable({ providedIn: 'root' })
export class DeploymentsService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/deployments';
  private readonly sseService = inject(DeploymentSseService);
  private readonly authService = inject(AuthService);
  private readonly refresh$ = new Subject<void>();
  readonly loading = signal(true);
  readonly loadError = signal<string | null>(null);

  readonly deployments$: Observable<Deployment[]> = this.refresh$.pipe(
    startWith(undefined),
    switchMap(() => {
      this.loading.set(true);
      this.loadError.set(null);
      return this.http.get<Deployment[]>(this.apiUrl).pipe(
        catchError(error => {
          this.loadError.set(this.authService.errorMessage(error));
          return of([]);
        }),
        finalize(() => this.loading.set(false)),
      );
    }),
    switchMap((initialDeployments) => new Observable<Deployment[]>((observer) => {
      let current = [...initialDeployments];
      observer.next(current);

      const changes$ = this.authService.hasRole('ADMIN') ? this.sseService.subscribeToAllDeployments() : NEVER;
      const subscription = changes$.subscribe((change) => {
        current = current.map((deployment) =>
          deployment.id === change.deploymentId
            ? { ...deployment, status: change.status as DeploymentStatus }
            : deployment,
        );
        observer.next(current);
      });

      return () => subscription.unsubscribe();
    })),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  refresh(): void {
    this.refresh$.next();
  }

  getById(id: string): Observable<DeploymentDetail> {
    return this.http.get<ApiResponse<DeploymentDetail>>(`${this.apiUrl}/${id}/details`).pipe(
      map((response) => response.data),
    );
  }

  getLogs(id: string): Observable<string> {
    return this.http.get<ApiResponse<string>>(`${this.apiUrl}/${id}/logs`).pipe(
      map((response) => response.data),
    );
  }

  create(value: DeploymentFormValue): Observable<Deployment> {
    const { projectName, ...request } = value;
    return this.http.post<ApiResponse<Deployment>>(this.apiUrl, request).pipe(
      map((response) => response.data),
      tap(() => this.refresh()),
    );
  }

  update(id: string, value: DeploymentFormValue): Observable<Deployment> {
    const { projectId, projectName, ...request } = value;
    return this.http.put<ApiResponse<Deployment>>(`${this.apiUrl}/${id}`, request).pipe(
      map((response) => response.data),
      tap(() => this.refresh()),
    );
  }

  rollback(id: string): Observable<Deployment> {
    return this.http.post<ApiResponse<Deployment>>(`${this.apiUrl}/${id}/rollback`, {}).pipe(
      map((response) => response.data),
      tap(() => this.refresh()),
    );
  }

  restart(id: string): Observable<Deployment> {
    return this.http.patch<ApiResponse<Deployment>>(`${this.apiUrl}/${id}/restart`, {}).pipe(
      map((response) => response.data),
      tap(() => this.refresh()),
    );
  }

  stop(id: string): Observable<Deployment> {
    return this.http.patch<ApiResponse<Deployment>>(`${this.apiUrl}/${id}/stop`, {}).pipe(
      map((response) => response.data),
      tap(() => this.refresh()),
    );
  }

  scale(id: string, replicas: number): Observable<Deployment> {
    return this.http.patch<ApiResponse<Deployment>>(`${this.apiUrl}/${id}/scale`, { replicas }).pipe(
      map((response) => response.data),
      tap(() => this.refresh()),
    );
  }

  delete(id: string): Observable<Deployment> {
    console.log('Deleting deployment service:', id);
    return this.http.delete<ApiResponse<Deployment>>(`${this.apiUrl}/${id}`).pipe(
      map(response => response.data),
      tap(() => this.refresh()),
    );
  }

  getOperationStatus(operationId: string): Observable<DeploymentJobResponse> {
    return this.http.get<ApiResponse<DeploymentJobResponse>>(`${this.apiUrl}/operations/${operationId}`).pipe(
      map(response => response.data)
    );
  }

  waitForOperation(operationId: string): Observable<DeploymentJobResponse> {
    return timer(0, 2000).pipe(
      switchMap(() => this.getOperationStatus(operationId)),
      filter(status => status.status === 'READY' || status.status === 'FAILED'),
      take(1)
    );
  }
}
