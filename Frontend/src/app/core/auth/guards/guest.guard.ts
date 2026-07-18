import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';

import { AuthService } from '../services/auth.service';

export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return auth.initialize().pipe(
    map((authenticated) =>
      authenticated ? router.createUrlTree(['/dashboard']) : true,
    ),
    catchError(() => of(true)),
  );
};
