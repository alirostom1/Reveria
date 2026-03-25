import { Component, HostListener, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
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
            <!-- Create button -->
            <button
              (click)="toggleCreateMenu($event)"
              class="flex h-9 w-9 items-center justify-center rounded-full border border-stone-200 text-stone-600 transition-all duration-200 hover:bg-stone-50 hover:text-stone-900"
              [class]="createMenuOpen() ? 'bg-stone-100 text-stone-900 border-stone-300' : ''"
            >
              <svg class="h-5 w-5 transition-transform duration-200" [class]="createMenuOpen() ? 'rotate-45' : ''" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
              </svg>
            </button>
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

    <!-- Create overlay -->
    @if (createMenuOpen()) {
      <div
        class="fixed inset-0 z-50 flex items-center justify-center"
        (click)="createMenuOpen.set(false)"
      >
        <!-- Backdrop -->
        <div class="absolute inset-0 bg-black/40 backdrop-blur-sm animate-fade-in"></div>

        <!-- Menu card -->
        <div
          class="relative z-10 w-full max-w-sm mx-4 animate-scale-in"
          (click)="$event.stopPropagation()"
        >
          <div class="overflow-hidden rounded-2xl border border-stone-200 bg-white shadow-2xl">
            <!-- Header -->
            <div class="border-b border-stone-100 px-5 py-4">
              <h2 class="text-base font-bold text-stone-900">Create</h2>
              <p class="mt-0.5 text-xs text-stone-500">What would you like to share?</p>
            </div>

            <!-- Actions -->
            <div class="p-2">
              <!-- Upload Video -->
              <button
                (click)="navigateTo('/explore/upload')"
                class="group flex w-full items-center gap-4 rounded-xl px-4 py-3.5 text-left transition-all duration-150 hover:bg-stone-50"
              >
                <div class="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-coral-50 text-coral-600 transition-colors group-hover:bg-coral-100">
                  <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.8">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M3 16.5v2.25A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75V16.5m-13.5-9L12 3m0 0 4.5 4.5M12 3v13.5" />
                  </svg>
                </div>
                <div>
                  <p class="text-sm font-semibold text-stone-900">Upload video</p>
                  <p class="mt-0.5 text-xs text-stone-500">Share a video with your audience</p>
                </div>
              </button>

              <!-- Start stream -->
              <button
                (click)="navigateTo('/live/go-live')"
                class="group flex w-full items-center gap-4 rounded-xl px-4 py-3.5 text-left transition-all duration-150 hover:bg-stone-50"
              >
                <div class="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-red-50 text-red-600 transition-colors group-hover:bg-red-100">
                  <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.8">
                    <path stroke-linecap="round" stroke-linejoin="round" d="m15.75 10.5 4.72-4.72a.75.75 0 0 1 1.28.53v11.38a.75.75 0 0 1-1.28.53l-4.72-4.72M4.5 18.75h9a2.25 2.25 0 0 0 2.25-2.25v-9a2.25 2.25 0 0 0-2.25-2.25h-9A2.25 2.25 0 0 0 2.25 7.5v9a2.25 2.25 0 0 0 2.25 2.25Z" />
                  </svg>
                </div>
                <div>
                  <p class="text-sm font-semibold text-stone-900">Go live</p>
                  <p class="mt-0.5 text-xs text-stone-500">Start streaming to your channel</p>
                </div>
              </button>

              <!-- Create post -->
              <button
                (click)="navigateTo('/explore')"
                class="group flex w-full items-center gap-4 rounded-xl px-4 py-3.5 text-left transition-all duration-150 hover:bg-stone-50"
              >
                <div class="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-sage-50 text-sage-600 transition-colors group-hover:bg-sage-100">
                  <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.8">
                    <path stroke-linecap="round" stroke-linejoin="round" d="m16.862 4.487 1.687-1.688a1.875 1.875 0 1 1 2.652 2.652L10.582 16.07a4.5 4.5 0 0 1-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 0 1 1.13-1.897l8.932-8.931Zm0 0L19.5 7.125M18 14v4.75A2.25 2.25 0 0 1 15.75 21H5.25A2.25 2.25 0 0 1 3 18.75V8.25A2.25 2.25 0 0 1 5.25 6H10" />
                  </svg>
                </div>
                <div>
                  <p class="text-sm font-semibold text-stone-900">Create post</p>
                  <p class="mt-0.5 text-xs text-stone-500">Share an update with your community</p>
                </div>
              </button>
            </div>
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    @keyframes fade-in {
      from { opacity: 0; }
      to { opacity: 1; }
    }
    @keyframes scale-in {
      from {
        opacity: 0;
        transform: scale(0.95) translateY(-8px);
      }
      to {
        opacity: 1;
        transform: scale(1) translateY(0);
      }
    }
    .animate-fade-in {
      animation: fade-in 0.15s ease-out;
    }
    .animate-scale-in {
      animation: scale-in 0.2s cubic-bezier(0.16, 1, 0.3, 1);
    }
  `],
})
export class Header {
  protected readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  protected readonly createMenuOpen = signal(false);

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.createMenuOpen.set(false);
  }

  protected toggleCreateMenu(event: Event): void {
    event.stopPropagation();
    this.createMenuOpen.update((v) => !v);
  }

  protected navigateTo(path: string): void {
    this.createMenuOpen.set(false);
    this.router.navigateByUrl(path);
  }
}
