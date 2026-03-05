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
  {
    path: 'settings',
    loadComponent: () => import('./pages/settings/settings').then((m) => m.Settings),
    children: [
      { path: '', redirectTo: 'password', pathMatch: 'full' },
      {
        path: 'password',
        loadComponent: () => import('./pages/change-password/change-password').then((m) => m.ChangePassword),
      },
      {
        path: 'sessions',
        loadComponent: () => import('./pages/sessions/sessions').then((m) => m.Sessions),
      },
      {
        path: 'accounts',
        loadComponent: () => import('./pages/connected-accounts/connected-accounts').then((m) => m.ConnectedAccounts),
      },
      {
        path: 'email',
        loadComponent: () => import('./pages/email-settings/email-settings').then((m) => m.EmailSettings),
      },
      {
        path: 'privacy',
        loadComponent: () => import('./pages/privacy-settings/privacy-settings').then((m) => m.PrivacySettingsPage),
      },
      {
        path: 'danger',
        loadComponent: () => import('./pages/danger-zone/danger-zone').then((m) => m.DangerZone),
      },
    ],
  },
];
