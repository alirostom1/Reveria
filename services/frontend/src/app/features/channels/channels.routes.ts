import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';

export const channelRoutes: Routes = [
  {
    path: 'me',
    loadComponent: () => import('./pages/my-channels/my-channels').then((m) => m.MyChannels),
    canActivate: [authGuard],
  },
  {
    path: 'create',
    loadComponent: () => import('./pages/create-channel/create-channel').then((m) => m.CreateChannel),
    canActivate: [authGuard],
  },
  {
    path: ':uuid',
    loadComponent: () => import('./pages/channel-detail/channel-detail').then((m) => m.ChannelDetail),
  },
  {
    path: ':uuid/edit',
    loadComponent: () => import('./pages/channel-edit/channel-edit').then((m) => m.ChannelEdit),
    canActivate: [authGuard],
  },
];
