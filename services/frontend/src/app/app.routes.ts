import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/home/pages/home/home').then((m) => m.Home),
  },
  {
    path: 'explore',
    loadChildren: () => import('./features/explore/explore.routes').then((m) => m.exploreRoutes),
  },
  {
    path: 'watch/:uuid',
    loadComponent: () => import('./features/watch/pages/watch/watch').then((m) => m.Watch),
  },
  {
    path: 'channels',
    loadChildren: () => import('./features/channels/channels.routes').then((m) => m.channelRoutes),
  },
  {
    path: 'live',
    loadChildren: () => import('./features/livestream/livestream.routes').then((m) => m.livestreamRoutes),
  },
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.routes').then((m) => m.authRoutes),
  },
  {
    path: 'profile',
    loadChildren: () => import('./features/profile/profile.routes').then((m) => m.profileRoutes),
    canActivate: [authGuard],
  },
  {
    path: '**',
    redirectTo: '',
  },
];
