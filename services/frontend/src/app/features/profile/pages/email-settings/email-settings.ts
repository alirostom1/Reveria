import { Component, inject, OnInit, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { environment } from '../../../../environments/environment';
import { ApiResponse, ApiError } from '../../../../core/models/api-response.model';
import { UserProfileResponse } from '../../../../core/models/profile.model';
import { ToastService } from '../../../../shared/components/toast/toast.service';

@Component({
  selector: 'app-email-settings',
  imports: [DatePipe],
  template: `
    <div class="space-y-6">
      <!-- Email Verification Card -->
      <div class="rounded-lg border border-stone-200 bg-white p-6 sm:p-8">
        <div class="mb-6">
          <h2 class="text-lg font-bold text-stone-900">Email verification</h2>
          <p class="mt-1 text-sm text-stone-500">Verify your email address to unlock all features</p>
        </div>

        @if (loading()) {
          <div class="flex flex-col items-center py-16">
            <div class="animate-spin mb-4 h-8 w-8 rounded-full border border-stone-200 border-t-stone-600"></div>
            <p class="text-sm text-stone-500">Loading…</p>
          </div>
        } @else if (profile()) {
          <div class="rounded-lg border p-5"
            [class]="profile()!.emailVerified
              ? 'border-emerald-200 bg-emerald-50/50'
              : 'border-amber-200 bg-amber-50/50'"
          >
            <div class="flex items-start gap-4">
              <div
                class="flex h-11 w-11 shrink-0 items-center justify-center rounded-lg"
                [class]="profile()!.emailVerified ? 'bg-emerald-100' : 'bg-amber-100'"
              >
                @if (profile()!.emailVerified) {
                  <svg class="h-6 w-6 text-emerald-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12c0 1.268-.63 2.39-1.593 3.068a3.745 3.745 0 01-1.043 3.296 3.745 3.745 0 01-3.296 1.043A3.745 3.745 0 0112 21c-1.268 0-2.39-.63-3.068-1.593a3.746 3.746 0 01-3.296-1.043 3.745 3.745 0 01-1.043-3.296A3.745 3.745 0 013 12c0-1.268.63-2.39 1.593-3.068a3.745 3.745 0 011.043-3.296 3.746 3.746 0 013.296-1.043A3.746 3.746 0 0112 3c1.268 0 2.39.63 3.068 1.593a3.746 3.746 0 013.296 1.043 3.746 3.746 0 011.043 3.296A3.745 3.745 0 0121 12z" />
                  </svg>
                } @else {
                  <svg class="h-6 w-6 text-amber-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z" />
                  </svg>
                }
              </div>
              <div class="flex-1">
                <p class="text-sm font-medium" [class]="profile()!.emailVerified ? 'text-emerald-800' : 'text-amber-800'">
                  {{ profile()!.emailVerified ? 'Email verified' : 'Email not verified' }}
                </p>
                <p class="mt-1 text-sm" [class]="profile()!.emailVerified ? 'text-emerald-700' : 'text-amber-700'">
                  {{ profile()!.email }}
                </p>
                @if (!profile()!.emailVerified) {
                  <p class="mt-2 text-xs text-amber-600">
                    Check your inbox for a verification link, or resend it below.
                  </p>
                }
              </div>
            </div>

            @if (!profile()!.emailVerified) {
              <div class="mt-4 border-t border-amber-200 pt-4">
                <button
                  (click)="resendVerification()"
                  [disabled]="resending()"
                  class="inline-flex items-center gap-2 rounded-lg bg-coral-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-coral-700 disabled:opacity-50"
                >
                  @if (resending()) {
                    <div class="animate-spin h-4 w-4 rounded-full border-2 border-white/30 border-t-white"></div>
                    Sending…
                  } @else {
                    <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M6 12L3.269 3.126A59.768 59.768 0 0121.485 12 59.77 59.77 0 013.27 20.876L5.999 12zm0 0h7.5" />
                    </svg>
                    Resend verification email
                  }
                </button>
              </div>
            }
          </div>
        }
      </div>

      <!-- Account Info Card -->
      <div class="rounded-lg border border-stone-200 bg-white p-6 sm:p-8">
        <div class="mb-5">
          <h2 class="text-lg font-bold text-stone-900">Account info</h2>
        </div>

        @if (!loading() && profile()) {
          <div class="space-y-3">
            <div class="flex items-center gap-3 rounded-lg border border-stone-200 p-4">
              <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-stone-100">
                <svg class="h-4 w-4 text-stone-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M21.75 6.75v10.5a2.25 2.25 0 01-2.25 2.25h-15a2.25 2.25 0 01-2.25-2.25V6.75m19.5 0A2.25 2.25 0 0019.5 4.5h-15a2.25 2.25 0 00-2.25 2.25m19.5 0v.243a2.25 2.25 0 01-1.07 1.916l-7.5 4.615a2.25 2.25 0 01-2.36 0L3.32 8.91a2.25 2.25 0 01-1.07-1.916V6.75" />
                </svg>
              </div>
              <div>
                <p class="text-xs text-stone-400">Email address</p>
                <p class="text-sm font-medium text-stone-900">{{ profile()!.email }}</p>
              </div>
            </div>
            <div class="flex items-center gap-3 rounded-lg border border-stone-200 p-4">
              <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-stone-100">
                <svg class="h-4 w-4 text-stone-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 012.25-2.25h13.5A2.25 2.25 0 0121 7.5v11.25m-18 0A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75m-18 0v-7.5A2.25 2.25 0 015.25 9h13.5A2.25 2.25 0 0121 11.25v7.5" />
                </svg>
              </div>
              <div>
                <p class="text-xs text-stone-400">Member since</p>
                <p class="text-sm font-medium text-stone-900">{{ profile()!.createdAt | date: 'MMMM d, y' }}</p>
              </div>
            </div>
          </div>
        }
      </div>
    </div>
  `,
})
export class EmailSettings implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);
  private readonly apiUrl = environment.apiUrl;

  protected readonly loading = signal(true);
  protected readonly resending = signal(false);
  protected readonly profile = signal<UserProfileResponse | null>(null);

  ngOnInit(): void {
    this.http.get<ApiResponse<UserProfileResponse>>(`${this.apiUrl}/api/profile/me`).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.data) this.profile.set(res.data);
      },
      error: () => this.loading.set(false),
    });
  }

  resendVerification(): void {
    const email = this.profile()?.email;
    if (!email) return;

    this.resending.set(true);
    this.http
      .post<ApiResponse<void>>(`${this.apiUrl}/api/auth/resend-verification`, { email })
      .subscribe({
        next: () => {
          this.resending.set(false);
          this.toast.success('Verification email sent! Check your inbox.');
        },
        error: (err) => {
          this.resending.set(false);
          const apiErr = err.error as ApiError | undefined;
          this.toast.error(apiErr?.message || 'Failed to resend verification email.');
        },
      });
  }
}

