import { Routes } from '@angular/router';
import { Applications } from './pages/applications/applications';
import { App } from './app';

export const routes: Routes = [
  { path: 'apps', component: Applications },
  { path: '', pathMatch: 'full', redirectTo: 'apps'},
];
