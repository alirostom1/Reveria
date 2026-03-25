import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';

export const livestreamRoutes: Routes = [
  {
    path: 'go-live',
    loadComponent: () => import('./pages/go-live/go-live').then((m) => m.GoLive),
    canActivate: [authGuard],
  },
  {
    path: ':uuid',
    loadComponent: () => import('./pages/watch-live/watch-live').then((m) => m.WatchLive),
  },
];
