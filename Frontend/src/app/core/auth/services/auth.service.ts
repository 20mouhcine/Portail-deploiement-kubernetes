import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import {
  catchError,
  finalize,
  map,
  Observable,
  of,
  shareReplay,
  switchMap,
  tap,
  throwError,
} from 'rxjs';

import {
  ApiError,
  ApiMessage,
  AuthStatus,
  CsrfResponse,
  CurrentUser,
  LoginCredentials,
  RoleName,
} from '../models/auth.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly currentUser = signal<CurrentUser | null>(null);
  private readonly currentStatus = signal<AuthStatus>('unknown');
  private initializationRequest?: Observable<boolean>;

  readonly user = this.currentUser.asReadonly();
  readonly status = this.currentStatus.asReadonly();
  readonly isAuthenticated = computed(
    () => this.currentStatus() === 'authenticated',
  );

  initialize(): Observable<boolean> {
    if (this.currentStatus() !== 'unknown') {
      return of(this.isAuthenticated());
    }

    if (!this.initializationRequest) {
      this.initializationRequest = this.refreshCsrfToken().pipe(
        switchMap(() => this.fetchCurrentUser()),
        map(() => true),
        catchError((error: HttpErrorResponse) => {
          this.clearSession();
          return error.status === 401 ? of(false) : throwError(() => error);
        }),
        finalize(() => {
          this.initializationRequest = undefined;
        }),
        shareReplay({ bufferSize: 1, refCount: true }),
      );
    }

    return this.initializationRequest;
  }

  login(credentials: LoginCredentials): Observable<CurrentUser> {
    const body = new HttpParams()
      .set('username', credentials.username.trim())
      .set('password', credentials.password);

    return this.refreshCsrfToken().pipe(
      switchMap(() =>
        this.http.post<ApiMessage>('/api/auth/login', body, {
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
          },
        }),
      ),
      switchMap(() => this.refreshCsrfToken()),
      switchMap(() => this.fetchCurrentUser()),
    );
  }

  logout(): Observable<void> {
    return this.http.post<ApiMessage>('/api/auth/logout', {}).pipe(
      tap(() => this.clearSession()),
      switchMap(() => this.refreshCsrfToken()),
      map(() => undefined),
      catchError((error: unknown) => {
        this.clearSession();
        return throwError(() => error);
      }),
    );
  }

  hasRole(role: RoleName): boolean {
    return this.currentUser()?.roles.includes(role) ?? false;
  }

  homeUrl(): string {
    return this.hasRole('ADMIN') ? '/admin/dashboard' : '/dashboard';
  }

  errorMessage(error: unknown): string {
    if (!(error instanceof HttpErrorResponse)) {
      return 'Une erreur inattendue est survenue.';
    }

    if (error.status === 0) {
      return 'Le serveur est indisponible. Vérifiez que le backend est démarré.';
    }

    const apiError = error.error as ApiError | null;
    return apiError?.message ?? 'La requête n’a pas pu être traitée.';
  }

  private fetchCurrentUser(): Observable<CurrentUser> {
    return this.http.get<CurrentUser>('/api/auth/me').pipe(
      tap((user) => {
        this.currentUser.set(user);
        this.currentStatus.set('authenticated');
      }),
    );
  }

  private refreshCsrfToken(): Observable<CsrfResponse> {
    return this.http.get<CsrfResponse>('/api/auth/csrf');
  }

  private clearSession(): void {
    this.currentUser.set(null);
    this.currentStatus.set('anonymous');
  }
}
