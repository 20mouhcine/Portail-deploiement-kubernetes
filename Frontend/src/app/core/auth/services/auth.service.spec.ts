import { provideHttpClient, withXsrfConfiguration } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { CurrentUser } from '../models/auth.models';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  const user: CurrentUser = {
    id: 1,
    username: 'admin',
    email: 'admin@example.com',
    enabled: true,
    createdAt: '2026-07-17T10:00:00',
    lastLogin: null,
    roles: ['ADMIN'],
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(
          withXsrfConfiguration({
            cookieName: 'XSRF-TOKEN',
            headerName: 'X-XSRF-TOKEN',
          }),
        ),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('initializes the authenticated user', () => {
    let authenticated = false;
    service.initialize().subscribe((value) => (authenticated = value));

    http.expectOne('/api/auth/csrf').flush({
      token: 'csrf-token',
      headerName: 'X-XSRF-TOKEN',
      parameterName: '_csrf',
    });
    http.expectOne('/api/auth/me').flush(user);

    expect(authenticated).toBe(true);
    expect(service.user()).toEqual(user);
    expect(service.isAuthenticated()).toBe(true);
  });

  it('performs the complete login flow', () => {
    let result: CurrentUser | undefined;
    service
      .login({ username: 'admin', password: 'CorrectPassword123!' })
      .subscribe((value) => (result = value));

    http.expectOne('/api/auth/csrf').flush({
      token: 'first-token',
      headerName: 'X-XSRF-TOKEN',
      parameterName: '_csrf',
    });

    const loginRequest = http.expectOne('/api/auth/login');
    expect(loginRequest.request.method).toBe('POST');
    expect(loginRequest.request.body.get('username')).toBe('admin');
    loginRequest.flush({ message: 'Connexion réussie' });

    http.expectOne('/api/auth/csrf').flush({
      token: 'second-token',
      headerName: 'X-XSRF-TOKEN',
      parameterName: '_csrf',
    });
    http.expectOne('/api/auth/me').flush(user);

    expect(result).toEqual(user);
    expect(service.isAuthenticated()).toBe(true);
  });
});
