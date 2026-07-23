import { Routes } from '@angular/router';
import { authGuard } from './core/auth/guards/auth.guard';
import { adminGuard } from './core/auth/guards/admin.guard';
import { guestGuard } from './core/auth/guards/guest.guard';
import { operationalGuard } from './core/auth/guards/operational.guard';

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
    path: '',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./layout/authenticated-layout/authenticated-layout').then(
        (module) => module.AuthenticatedLayout,
      ),
    children: [
      {
        path: 'dashboard',
        canActivate: [operationalGuard],
        loadComponent: () =>
          import('./features/dashboard/pages/dashboard-page/dashboard-page').then(
            (module) => module.DashboardPage,
          ),
        title: 'Tableau de bord | KubePortal',
      },
      {
        path: 'admin/dashboard',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./features/admin/dashboard/pages/admin-dashboard-page/admin-dashboard-page').then(
            (module) => module.AdminDashboardPage,
          ),
        title: 'Administration | KubePortal',
      },
      {
        path: 'admin/users/new',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./features/admin/users/pages/user-registration-page/user-registration-page').then(
            (module) => module.UserRegistrationPage,
          ),
        title: 'Gestion des utilisateurs | KubePortal',
      },
      {
        path: 'projects',
        loadComponent: () =>
          import('./features/projects/pages/projects-page/projects').then(
            (module) => module.Projects,
          ),
        title: 'Projects | KubePortal',
      },
      {
        path: 'deployments',
        loadComponent: () =>
          import('./features/deployments/pages/deployments-page/deployments-page').then(
            (module) => module.DeploymentsPage,
          ),
        title: 'Déploiements | KubePortal',
      },
      {
        path: 'history',
        loadComponent: () =>
          import('./features/history/pages/history-page/history-page').then(
            (module) => module.HistoryPage,
          ),
        title: 'Historique | KubePortal',
      },
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard',
      },
    ],
  },
  {
    path: '**',
    redirectTo: 'dashboard',
  },
];
