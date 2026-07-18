import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';

import { AuthService } from '../services/auth.service';

export const operationalGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return auth.initialize().pipe(
    map((authenticated) => {
      if (!authenticated) {
        return router.createUrlTree(['/login']);
      }

      return auth.hasRole('ADMIN')
        ? router.createUrlTree(['/admin/dashboard'])
        : true;
    }),
    catchError(() => of(router.createUrlTree(['/login']))),
  );
};
