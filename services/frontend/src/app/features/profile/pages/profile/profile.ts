import { Component, inject, OnInit, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../../core/models/api-response.model';
import { UserProfileResponse } from '../../../../core/models/profile.model';
import { DatePipe } from '@angular/common';

@Component({
  selector: 'app-profile',
  imports: [RouterLink, DatePipe],
  template: `
    <div class="animate-fade-up mx-auto max-w-3xl px-4 py-12">
      @if (loading()) {
        <div class="flex flex-col items-center py-24">
          <div class="animate-spin-slow mb-5 h-10 w-10 rounded-full border-4 border-sand-200 border-t-coral-600"></div>
          <p class="text-base text-stone-500 font-medium">Loading profile...</p>
        </div>
      } @else if (profile()) {
        <!-- Profile Card -->
        <div class="overflow-hidden rounded-3xl border-2 border-sand-200 bg-white shadow-2xl">
          <!-- Gradient Banner -->
          <div class="relative h-40 bg-gradient-to-br from-coral-400 via-coral-500 to-sage-500">
            <div class="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iODAiIGhlaWdodD0iODAiIHZpZXdCb3g9IjAgMCA4MCA4MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48ZyBmaWxsPSJub25lIiBmaWxsLXJ1bGU9ImV2ZW5vZGQiPjxjaXJjbGUgY3g9IjQwIiBjeT0iNDAiIHI9IjMiIHN0cm9rZT0icmdiYSgyNTUsMjU1LDI1NSwwLjEpIiBzdHJva2Utd2lkdGg9IjEuNSIvPjwvZz48L3N2Zz4=')] opacity-30"></div>
          </div>

          <!-- Avatar + Info -->
          <div class="px-8 pb-8">
            <div class="flex flex-col sm:flex-row sm:items-end sm:justify-between">
              <div class="flex items-end gap-6 -mt-14">
                @if (profile()!.avatarUrl) {
                  <img
                    [src]="profile()!.avatarUrl!"
                    alt="Avatar"
                    class="h-28 w-28 rounded-2xl border-4 border-white object-cover shadow-2xl ring-4 ring-sand-100"
                  />
                } @else {
                  <div
                    class="flex h-28 w-28 items-center justify-center rounded-2xl border-4 border-white bg-gradient-to-br from-coral-500 to-coral-600 text-4xl font-bold text-white shadow-2xl ring-4 ring-sand-100"
                  >
                    {{ profile()!.displayName.charAt(0).toUpperCase() }}
                  </div>
                }
                <div class="mb-2">
                  <h1 class="text-2xl font-bold text-stone-900">{{ profile()!.displayName }}</h1>
                  <p class="text-base text-stone-500 font-medium">&#64;{{ profile()!.username }}</p>
                </div>
              </div>
              <a
                routerLink="/profile/edit"
                class="mt-5 inline-flex items-center gap-2.5 self-start rounded-xl border-2 border-sand-200 bg-white px-5 py-2.5 text-sm font-semibold text-stone-700 shadow-md transition-all duration-150 hover:border-sand-300 hover:shadow-lg sm:mt-0 sm:self-auto"
              >
                <svg class="h-5 w-5 text-coral-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931zm0 0L19.5 7.125M18 14v4.75A2.25 2.25 0 0115.75 21H5.25A2.25 2.25 0 013 18.75V8.25A2.25 2.25 0 015.25 6H10" />
                </svg>
                Edit profile
              </a>
            </div>

            @if (profile()!.bio) {
              <p class="mt-6 text-base leading-relaxed text-stone-700">{{ profile()!.bio }}</p>
            }

            <!-- Details -->
            <div class="mt-8 space-y-5 border-t-2 border-sand-100 pt-8">
              <div class="flex items-center gap-4">
                <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-coral-100">
                  <svg class="h-5 w-5 text-coral-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M21.75 6.75v10.5a2.25 2.25 0 01-2.25 2.25h-15a2.25 2.25 0 01-2.25-2.25V6.75m19.5 0A2.25 2.25 0 0019.5 4.5h-15a2.25 2.25 0 00-2.25 2.25m19.5 0v.243a2.25 2.25 0 01-1.07 1.916l-7.5 4.615a2.25 2.25 0 01-2.36 0L3.32 8.91a2.25 2.25 0 01-1.07-1.916V6.75" />
                  </svg>
                </div>
                <div class="flex items-center gap-2.5">
                  <span class="text-base font-medium text-stone-900">{{ profile()!.email }}</span>
                  @if (profile()!.emailVerified) {
                    <span class="inline-flex items-center gap-1.5 rounded-full bg-emerald-100 px-3 py-1 text-xs font-bold text-emerald-700 ring-2 ring-emerald-200">
                      <svg class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="3">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5" />
                      </svg>
                      Verified
                    </span>
                  } @else {
                    <span class="inline-flex items-center rounded-full bg-amber-100 px-3 py-1 text-xs font-bold text-amber-700 ring-2 ring-amber-200">
                      Unverified
                    </span>
                  }
                </div>
              </div>

              <div class="flex items-center gap-4">
                <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-sage-100">
                  <svg class="h-5 w-5 text-sage-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 012.25-2.25h13.5A2.25 2.25 0 0121 7.5v11.25m-18 0A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75m-18 0v-7.5A2.25 2.25 0 015.25 9h13.5A2.25 2.25 0 0121 11.25v7.5" />
                  </svg>
                </div>
                <span class="text-base font-medium text-stone-900">Joined {{ profile()!.createdAt | date: 'MMMM d, y' }}</span>
              </div>

              @if (profile()!.linkedProviders.length) {
                <div class="flex items-center gap-4">
                  <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-sand-200">
                    <svg class="h-5 w-5 text-sand-700" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M13.19 8.688a4.5 4.5 0 011.242 7.244l-4.5 4.5a4.5 4.5 0 01-6.364-6.364l1.757-1.757m13.35-.622l1.757-1.757a4.5 4.5 0 00-6.364-6.364l-4.5 4.5a4.5 4.5 0 001.242 7.244" />
                    </svg>
                  </div>
                  <div class="flex flex-wrap gap-2">
                    @for (provider of profile()!.linkedProviders; track provider) {
                      <span class="inline-flex items-center rounded-xl bg-sand-100 px-3.5 py-1.5 text-xs font-bold text-stone-700 ring-2 ring-sand-200">
                        {{ provider }}
                      </span>
                    }
                  </div>
                </div>
              }
            </div>
          </div>
        </div>
      }
    </div>
  `,
})
export class Profile implements OnInit {
  private readonly http = inject(HttpClient);

  protected readonly loading = signal(true);
  protected readonly profile = signal<UserProfileResponse | null>(null);

  ngOnInit(): void {
    this.http.get<ApiResponse<UserProfileResponse>>(`${environment.apiUrl}/api/profile/me`).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.data) this.profile.set(res.data);
      },
      error: () => this.loading.set(false),
    });
  }
}
