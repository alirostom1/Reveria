import { Component, inject, OnInit, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../../core/models/api-response.model';
import { UserProfileResponse } from '../../../../core/models/profile.model';

@Component({
  selector: 'app-profile',
  imports: [RouterLink],
  template: `
    <div>
      @if (loading()) {
        <div class="flex flex-col items-center py-32">
          <div class="h-8 w-8 animate-spin rounded-full border-2 border-stone-200 border-t-stone-600"></div>
        </div>
      } @else if (profile()) {
        <!-- Banner -->
        <div class="h-36 bg-coral-600 sm:h-44"></div>

        <!-- Profile Header -->
        <div class="mx-auto max-w-3xl px-4 sm:px-6">
          <div class="relative -mt-12 sm:-mt-14">
            @if (profile()!.avatarUrl) {
              <img
                [src]="profile()!.avatarUrl!"
                alt="Avatar"
                referrerpolicy="no-referrer"
                class="h-24 w-24 shrink-0 rounded-full border-4 border-white object-cover sm:h-28 sm:w-28"
              />
            } @else {
              <div
                class="flex h-24 w-24 shrink-0 items-center justify-center rounded-full border-4 border-white bg-coral-600 text-3xl font-bold text-white sm:h-28 sm:w-28 sm:text-4xl"
              >
                {{ profile()!.displayName.charAt(0).toUpperCase() }}
              </div>
            }

            <div class="mt-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <h1 class="text-xl font-bold text-stone-900 sm:text-2xl">
                    {{ profile()!.displayName }}
                  </h1>
                  <p class="text-sm text-stone-500">&#64;{{ profile()!.username }}</p>
                </div>
                <div class="flex gap-2">
                  <a
                    routerLink="/profile/edit"
                    class="inline-flex items-center gap-1.5 rounded-lg border border-stone-200 bg-white px-4 py-2 text-sm font-medium text-stone-700 transition-colors hover:border-stone-300 hover:bg-stone-50"
                  >
                    Edit profile
                  </a>
                </div>
            </div>

            @if (profile()!.bio) {
              <p class="mt-4 max-w-xl text-sm text-stone-600">{{ profile()!.bio }}</p>
            }
          </div>

          <!-- Tabs -->
          <div class="mt-6 border-b border-stone-200">
            <nav class="flex gap-0">
              <button
                (click)="activeTab.set('videos')"
                class="relative px-4 py-3 text-sm font-medium transition-colors"
                [class]="activeTab() === 'videos'
                  ? 'text-stone-900'
                  : 'text-stone-400 hover:text-stone-600'"
              >
                Videos
                @if (activeTab() === 'videos') {
                  <span class="absolute bottom-0 left-0 right-0 h-0.5 bg-stone-900"></span>
                }
              </button>
              <button
                (click)="activeTab.set('posts')"
                class="relative px-4 py-3 text-sm font-medium transition-colors"
                [class]="activeTab() === 'posts'
                  ? 'text-stone-900'
                  : 'text-stone-400 hover:text-stone-600'"
              >
                Posts
                @if (activeTab() === 'posts') {
                  <span class="absolute bottom-0 left-0 right-0 h-0.5 bg-stone-900"></span>
                }
              </button>
            </nav>
          </div>

          <!-- Tab Content -->
          <div class="py-16">
            <div class="flex flex-col items-center justify-center text-center">
              <svg class="mb-3 h-10 w-10 text-stone-300" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.2">
                @if (activeTab() === 'videos') {
                  <path stroke-linecap="round" stroke-linejoin="round" d="m15.75 10.5 4.72-4.72a.75.75 0 0 1 1.28.53v11.38a.75.75 0 0 1-1.28.53l-4.72-4.72M4.5 18.75h9a2.25 2.25 0 0 0 2.25-2.25v-9a2.25 2.25 0 0 0-2.25-2.25h-9A2.25 2.25 0 0 0 2.25 7.5v9a2.25 2.25 0 0 0 2.25 2.25Z" />
                } @else {
                  <path stroke-linecap="round" stroke-linejoin="round" d="M7.5 8.25h9m-9 3H12m-9.75 1.51c0 1.6 1.123 2.994 2.707 3.227 1.129.166 2.27.293 3.423.379.35.026.67.21.865.501L12 21l2.755-4.133a1.14 1.14 0 01.865-.501 48.172 48.172 0 003.423-.379c1.584-.233 2.707-1.626 2.707-3.228V6.741c0-1.602-1.123-2.995-2.707-3.228A48.394 48.394 0 0012 3c-2.392 0-4.744.175-7.043.513C3.373 3.746 2.25 5.14 2.25 6.741v6.018z" />
                }
              </svg>
              <p class="text-sm font-medium text-stone-500">No {{ activeTab() }} yet</p>
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
  protected readonly activeTab = signal<'videos' | 'posts'>('videos');

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
