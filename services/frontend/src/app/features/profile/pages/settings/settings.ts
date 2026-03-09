import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-settings',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <div class="mx-auto max-w-3xl px-4 py-8 sm:px-6">
      <div class="mb-6">
        <h1 class="text-2xl font-bold text-stone-900">Settings</h1>
        <p class="mt-1 text-sm text-stone-500">Manage your account, security, and privacy</p>
      </div>

      <div class="flex flex-col gap-6 lg:flex-row">
        <nav class="flex flex-row gap-0.5 overflow-x-auto rounded-lg border border-stone-200 bg-white p-1 lg:w-48 lg:flex-col lg:overflow-visible">
          <a routerLink="password" routerLinkActive="bg-stone-100 text-stone-900"
            class="whitespace-nowrap rounded-md px-3 py-2 text-sm font-medium text-stone-500 transition-colors hover:text-stone-900">
            Password
          </a>
          <a routerLink="sessions" routerLinkActive="bg-stone-100 text-stone-900"
            class="whitespace-nowrap rounded-md px-3 py-2 text-sm font-medium text-stone-500 transition-colors hover:text-stone-900">
            Sessions
          </a>
          <a routerLink="accounts" routerLinkActive="bg-stone-100 text-stone-900"
            class="whitespace-nowrap rounded-md px-3 py-2 text-sm font-medium text-stone-500 transition-colors hover:text-stone-900">
            Accounts
          </a>
          <a routerLink="email" routerLinkActive="bg-stone-100 text-stone-900"
            class="whitespace-nowrap rounded-md px-3 py-2 text-sm font-medium text-stone-500 transition-colors hover:text-stone-900">
            Email
          </a>
          <a routerLink="privacy" routerLinkActive="bg-stone-100 text-stone-900"
            class="whitespace-nowrap rounded-md px-3 py-2 text-sm font-medium text-stone-500 transition-colors hover:text-stone-900">
            Privacy
          </a>
          <a routerLink="danger" routerLinkActive="bg-red-50 text-red-600"
            class="whitespace-nowrap rounded-md px-3 py-2 text-sm font-medium text-stone-500 transition-colors hover:text-red-600">
            Danger zone
          </a>
        </nav>

        <div class="min-w-0 flex-1">
          <router-outlet />
        </div>
      </div>
    </div>
  `,
})
export class Settings {}

