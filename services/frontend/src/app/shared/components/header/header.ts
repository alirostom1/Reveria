import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { UserMenu } from '../user-menu/user-menu';

@Component({
  selector: 'app-header',
  imports: [RouterLink, RouterLinkActive, UserMenu],
  template: `
    <header
      class="sticky top-0 z-40 border-b border-sand-200 bg-white/80 backdrop-blur-xl"
    >
      <nav class="mx-auto flex h-18 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
        <a routerLink="/" class="flex items-center gap-3">
          <div
            class="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-coral-500 to-coral-600 shadow-lg shadow-coral-500/25"
          >
            <svg class="h-5.5 w-5.5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09zM18.259 8.715L18 9.75l-.259-1.035a3.375 3.375 0 00-2.455-2.456L14.25 6l1.036-.259a3.375 3.375 0 002.455-2.456L18 2.25l.259 1.035a3.375 3.375 0 002.455 2.456L21.75 6l-1.036.259a3.375 3.375 0 00-2.455 2.456z" />
            </svg>
          </div>
          <span class="text-xl font-bold tracking-tight text-stone-900">Reveria</span>
        </a>

        <div class="flex items-center gap-3">
          @if (authService.isAuthenticated()) {
            <app-user-menu />
          } @else {
            <a
              routerLink="/auth/login"
              routerLinkActive="text-coral-600"
              class="rounded-lg px-4 py-2 text-sm font-medium text-stone-600 transition-colors duration-150 hover:text-stone-900"
            >
              Log in
            </a>
            <a
              routerLink="/auth/register"
              class="btn-shine rounded-xl bg-gradient-to-r from-coral-500 to-coral-600 px-5 py-2.5 text-sm font-semibold text-white shadow-lg shadow-coral-500/25 transition-all duration-200 hover:shadow-xl hover:shadow-coral-500/30 hover:-translate-y-0.5"
            >
              Get started
            </a>
          }
        </div>
      </nav>
    </header>
  `,
})
export class Header {
  protected readonly authService = inject(AuthService);
}
