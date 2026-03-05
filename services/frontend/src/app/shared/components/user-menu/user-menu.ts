import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-user-menu',
  imports: [RouterLink],
  template: `
    <div class="relative">
      <button
        (click)="open.set(!open())"
        class="flex items-center gap-2 rounded-lg border border-stone-200 bg-white px-2.5 py-1.5 text-sm font-medium text-stone-700 transition-colors hover:border-stone-300"
      >
        @if (authService.currentUser()?.avatarUrl) {
          <img
            [src]="authService.currentUser()!.avatarUrl!"
            alt="Avatar"
            class="h-6 w-6 rounded-md object-cover"
          />
        } @else {
          <span
            class="flex h-6 w-6 items-center justify-center rounded-md bg-coral-600 text-xs font-bold text-white"
          >
            {{ authService.currentUser()?.displayName?.charAt(0)?.toUpperCase() || 'U' }}
          </span>
        }
        <span class="hidden sm:inline">{{ authService.currentUser()?.displayName || 'User' }}</span>
        <svg
          class="h-3.5 w-3.5 text-stone-400 transition-transform duration-150"
          [class.rotate-180]="open()"
          fill="none" stroke="currentColor" viewBox="0 0 24 24"
        >
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
        </svg>
      </button>

      @if (open()) {
        <div
          class="animate-dropdown absolute right-0 mt-1.5 w-48 overflow-hidden rounded-lg border border-stone-200 bg-white p-1 shadow-lg"
        >
          <a
            routerLink="/profile"
            (click)="open.set(false)"
            class="flex items-center gap-2.5 rounded-md px-3 py-2 text-sm text-stone-700 transition-colors hover:bg-stone-50"
          >
            <svg class="h-4 w-4 text-stone-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.8">
              <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z" />
            </svg>
            Profile
          </a>
          <a
            routerLink="/profile/settings"
            (click)="open.set(false)"
            class="flex items-center gap-2.5 rounded-md px-3 py-2 text-sm text-stone-700 transition-colors hover:bg-stone-50"
          >
            <svg class="h-4 w-4 text-stone-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.8">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9.594 3.94c.09-.542.56-.94 1.11-.94h2.593c.55 0 1.02.398 1.11.94l.213 1.281c.063.374.313.686.645.87.074.04.147.083.22.127.325.196.72.257 1.075.124l1.217-.456a1.125 1.125 0 011.37.49l1.296 2.247a1.125 1.125 0 01-.26 1.431l-1.003.827c-.293.241-.438.613-.43.992a6.759 6.759 0 010 .255c-.007.378.138.75.43.991l1.004.827c.424.35.534.955.26 1.43l-1.298 2.247a1.125 1.125 0 01-1.369.491l-1.217-.456c-.355-.133-.75-.072-1.076.124a6.57 6.57 0 01-.22.128c-.331.183-.581.495-.644.869l-.213 1.28c-.09.543-.56.941-1.11.941h-2.594c-.55 0-1.02-.398-1.11-.94l-.213-1.281c-.062-.374-.312-.686-.644-.87a6.52 6.52 0 01-.22-.127c-.325-.196-.72-.257-1.076-.124l-1.217.456a1.125 1.125 0 01-1.369-.49l-1.297-2.247a1.125 1.125 0 01.26-1.431l1.004-.827c.292-.24.437-.613.43-.991a6.932 6.932 0 010-.255c.007-.378-.138-.75-.43-.992l-1.004-.827a1.125 1.125 0 01-.26-1.43l1.297-2.247a1.125 1.125 0 011.37-.491l1.216.456c.356.133.751.072 1.076-.124.072-.044.146-.087.22-.128.332-.183.582-.495.644-.869l.214-1.281z" />
              <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            Settings
          </a>
          <div class="my-1 border-t border-stone-100"></div>
          <button
            (click)="logout()"
            class="flex w-full items-center gap-2.5 rounded-md px-3 py-2 text-sm text-stone-700 transition-colors hover:bg-red-50 hover:text-red-600"
          >
            <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.8">
              <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15m3 0l3-3m0 0l-3-3m3 3H9" />
            </svg>
            Log out
          </button>
        </div>
      }
    </div>
  `,
  host: {
    '(document:click)': 'onDocumentClick($event)',
  },
})
export class UserMenu {
  protected readonly authService = inject(AuthService);
  protected readonly open = signal(false);

  logout(): void {
    this.open.set(false);
    this.authService.logout();
  }

  onDocumentClick(event: Event): void {
    const el = event.target as HTMLElement;
    if (!el.closest('app-user-menu')) {
      this.open.set(false);
    }
  }
}
