import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';

export const exploreRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/explore/explore').then((m) => m.Explore),
  },
  {
    path: 'upload',
    loadComponent: () => import('./pages/upload-video/upload-video').then((m) => m.UploadVideo),
    canActivate: [authGuard],
  },
];
