import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';

export const socialRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/feed/feed').then((m) => m.Feed),
    canActivate: [authGuard],
  },
  {
    path: 'friends',
    loadComponent: () => import('./pages/friends/friends').then((m) => m.Friends),
    canActivate: [authGuard],
  },
  {
    path: 'user/:uuid',
    loadComponent: () => import('./pages/user-profile/user-profile').then((m) => m.UserProfile),
    canActivate: [authGuard],
  },
  {
    path: 'story/:uuid',
    loadComponent: () => import('./pages/story-viewer/story-viewer').then((m) => m.StoryViewer),
    canActivate: [authGuard],
  },
];
