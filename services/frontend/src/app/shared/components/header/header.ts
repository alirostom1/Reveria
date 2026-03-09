import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { UserMenu } from '../user-menu/user-menu';

@Component({
  selector: 'app-header',
  imports: [RouterLink, UserMenu],
  template: `
    <header class="fixed top-0 left-0 right-0 z-40 h-14 border-b border-stone-200 bg-white">
      <nav class="flex h-full items-center justify-between px-4 sm:px-6">
        <!-- Logo -->
        <a routerLink="/" class="flex items-center">
          <span class="text-xl font-bold tracking-tight text-stone-900">reveria</span>
        </a>

        <!-- Search -->
        <div class="mx-4 hidden max-w-md flex-1 sm:block">
          <div class="relative">
            <svg class="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-stone-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="m21 21-5.197-5.197m0 0A7.5 7.5 0 1 0 5.196 5.196a7.5 7.5 0 0 0 10.607 10.607Z" />
            </svg>
            <input
              type="text"
              placeholder="Search"
              class="h-9 w-full rounded-lg border border-stone-200 bg-stone-50 pl-9 pr-4 text-sm text-stone-900 placeholder:text-stone-400 transition-colors focus:border-stone-300 focus:bg-white focus:outline-none"
            />
          </div>
        </div>

        <!-- Right side -->
        <div class="flex items-center gap-2">
          @if (authService.isAuthenticated()) {
            <app-user-menu />
          } @else {
            <a
              routerLink="/auth/login"
              class="rounded-lg px-3.5 py-1.5 text-sm font-medium text-stone-600 transition-colors hover:text-stone-900"
            >
              Log in
            </a>
            <a
              routerLink="/auth/register"
              class="rounded-lg bg-stone-900 px-3.5 py-1.5 text-sm font-medium text-white transition-colors hover:bg-stone-800"
            >
              Sign up
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
