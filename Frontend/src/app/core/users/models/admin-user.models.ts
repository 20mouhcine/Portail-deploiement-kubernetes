import { RoleName } from '../../auth/models/auth.models';

export interface CreateUserRequest {
  readonly username: string;
  readonly email: string;
  readonly password: string;
  readonly roles: readonly RoleName[];
}
