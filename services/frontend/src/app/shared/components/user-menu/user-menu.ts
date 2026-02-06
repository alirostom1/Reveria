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
        class="flex items-center gap-2.5 rounded-xl border border-sand-200 bg-white px-3 py-2 text-sm font-medium text-stone-700 shadow-sm transition-all duration-150 hover:border-sand-300 hover:shadow-md"
      >
        @if (authService.currentUser()?.avatarUrl) {
          <img
            [src]="authService.currentUser()!.avatarUrl!"
            alt="Avatar"
            class="h-7 w-7 rounded-lg object-cover ring-2 ring-white"
          />
        } @else {
          <span
            class="flex h-7 w-7 items-center justify-center rounded-lg bg-gradient-to-br from-coral-500 to-coral-600 text-xs font-bold text-white"
          >
            {{ authService.currentUser()?.displayName?.charAt(0)?.toUpperCase() || 'U' }}
          </span>
        }
        <span class="hidden sm:inline">{{ authService.currentUser()?.displayName || 'User' }}</span>
        <svg
          class="h-4 w-4 text-stone-400 transition-transform duration-150"
          [class.rotate-180]="open()"
          fill="none" stroke="currentColor" viewBox="0 0 24 24"
        >
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
        </svg>
      </button>

      @if (open()) {
        <div
          class="animate-dropdown absolute right-0 mt-2 w-56 overflow-hidden rounded-2xl border border-sand-200 bg-white p-2 shadow-2xl"
        >
          <a
            routerLink="/profile"
            (click)="open.set(false)"
            class="flex items-center gap-3 rounded-xl px-3.5 py-3 text-sm font-medium text-stone-700 transition-colors duration-100 hover:bg-sand-50"
          >
            <svg class="h-5 w-5 text-coral-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z" />
            </svg>
            Profile
          </a>
          <div class="my-1.5 border-t border-sand-100"></div>
          <button
            (click)="logout()"
            class="flex w-full items-center gap-3 rounded-xl px-3.5 py-3 text-sm font-medium text-stone-700 transition-colors duration-100 hover:bg-red-50 hover:text-red-600"
          >
            <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
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
