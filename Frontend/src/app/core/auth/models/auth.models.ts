export type RoleName = 'ADMIN' | 'DEVOPS' | 'DEVELOPER';

export interface CurrentUser {
  readonly id: number;
  readonly username: string;
  readonly email: string;
  readonly enabled: boolean;
  readonly createdAt: string;
  readonly lastLogin: string | null;
  readonly roles: readonly RoleName[];
}

export interface LoginCredentials {
  readonly username: string;
  readonly password: string;
}

export interface CsrfResponse {
  readonly token: string;
  readonly headerName: string;
  readonly parameterName: string;
}

export interface ApiMessage {
  readonly message: string;
}

export interface ApiError {
  readonly message?: string;
  readonly fieldErrors?: Readonly<Record<string, string>>;
}

export type AuthStatus = 'unknown' | 'authenticated' | 'anonymous';
