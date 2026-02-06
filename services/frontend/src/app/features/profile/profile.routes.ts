import { Routes } from '@angular/router';

export const profileRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/profile/profile').then((m) => m.Profile),
  },
  {
    path: 'edit',
    loadComponent: () => import('./pages/profile-edit/profile-edit').then((m) => m.ProfileEdit),
  },
];
