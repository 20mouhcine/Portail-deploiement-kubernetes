import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { CurrentUser } from '../../auth/models/auth.models';
import {
  CreateUserRequest,
  UpdateUserRolesRequest,
  UpdateUserStatusRequest,
} from '../models/admin-user.models';

@Injectable({ providedIn: 'root' })
export class AdminUserService {
  private readonly http = inject(HttpClient);

  createUser(request: CreateUserRequest): Observable<CurrentUser> {
    return this.http.post<CurrentUser>('/api/admin/users', request);
  }

  getUsers(): Observable<readonly CurrentUser[]> {
    return this.http.get<readonly CurrentUser[]>('/api/admin/users');
  }

  updateRoles(userId: string, request: UpdateUserRolesRequest): Observable<CurrentUser> {
    return this.http.put<CurrentUser>(`/api/admin/users/${userId}/roles`, request);
  }

  setEnabled(userId: string, request: UpdateUserStatusRequest): Observable<CurrentUser> {
    return this.http.patch<CurrentUser>(`/api/admin/users/${userId}/enabled`, request);
  }
}
