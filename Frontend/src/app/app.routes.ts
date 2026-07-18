import { Routes } from '@angular/router';
import { Applications } from './pages/applications/applications';
import { authGuard } from './core/auth/guards/auth.guard';
import { guestGuard } from './core/auth/guards/guest.guard';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./features/auth/pages/login-page/login-page').then(
        (module) => module.LoginPage,
      ),
    title: 'Connexion | KubePortal',
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/dashboard/pages/dashboard-page/dashboard-page').then(
        (module) => module.DashboardPage,
      ),
    title: 'Tableau de bord | KubePortal',
  },
  {
    path: 'apps',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/applications/applications').then(
        (module) => module.Applications,
      ),
    title: 'Applications | KubePortal',
  },
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'dashboard',
  },
  {
    path: '**',
    redirectTo: 'dashboard',
  },
];
