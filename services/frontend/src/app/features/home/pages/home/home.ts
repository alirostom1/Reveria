import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-home',
  imports: [RouterLink],
  template: `
    <div class="mx-auto max-w-3xl px-4 py-8 sm:px-6">
      @if (!authService.isAuthenticated()) {
        <!-- Signed-out welcome -->
        <div class="mb-10">
          <h1 class="text-2xl font-bold tracking-tight text-stone-900">Welcome to Reveria</h1>
          <p class="mt-2 text-sm text-stone-500">Discover videos and posts from creators around the world.</p>
          <div class="mt-5 flex gap-2.5">
            <a
              routerLink="/auth/register"
              class="rounded-lg bg-stone-900 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-stone-800"
            >
              Sign up
            </a>
            <a
              routerLink="/auth/login"
              class="rounded-lg border border-stone-200 bg-white px-4 py-2 text-sm font-medium text-stone-700 transition-colors hover:border-stone-300 hover:bg-stone-50"
            >
              Log in
            </a>
          </div>
        </div>
      } @else {
        <!-- Signed-in greeting -->
        <div class="mb-8">
          <h1 class="text-2xl font-bold tracking-tight text-stone-900">
            Welcome back{{ authService.currentUser()?.displayName ? ', ' + authService.currentUser()!.displayName.split(' ')[0] : '' }}
          </h1>
          <p class="mt-1 text-sm text-stone-500">Here's what's new in your feed.</p>
        </div>
      }

      <!-- Feed -->
      <div>
        <div class="flex flex-col items-center justify-center rounded-xl border border-dashed border-stone-200 py-20 text-center">
          <svg class="mb-4 h-10 w-10 text-stone-300" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.2">
            <path stroke-linecap="round" stroke-linejoin="round" d="m15.75 10.5 4.72-4.72a.75.75 0 0 1 1.28.53v11.38a.75.75 0 0 1-1.28.53l-4.72-4.72M4.5 18.75h9a2.25 2.25 0 0 0 2.25-2.25v-9a2.25 2.25 0 0 0-2.25-2.25h-9A2.25 2.25 0 0 0 2.25 7.5v9a2.25 2.25 0 0 0 2.25 2.25Z" />
          </svg>
          <p class="text-sm font-medium text-stone-500">Nothing here yet</p>
          <p class="mt-1 text-xs text-stone-400">Content from creators you follow will show up here.</p>
        </div>
      </div>
    </div>
  `,
})
export class Home {
  protected readonly authService = inject(AuthService);
}
