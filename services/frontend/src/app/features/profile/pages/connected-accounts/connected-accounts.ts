import { Component, inject, OnInit, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { environment } from '../../../../environments/environment';
import { ApiResponse, ApiError } from '../../../../core/models/api-response.model';
import { LinkedProviderResponse } from '../../../../core/models/profile.model';
import { AuthService } from '../../../../core/services/auth.service';
import { ToastService } from '../../../../shared/components/toast/toast.service';

const PROVIDERS = ['GOOGLE', 'GITHUB', 'FACEBOOK'] as const;

@Component({
  selector: 'app-connected-accounts',
  imports: [DatePipe],
  template: `
    <div class="rounded-lg border border-stone-200 bg-white p-6 sm:p-8">
      <div class="mb-6">
        <h2 class="text-lg font-bold text-stone-900">Connected accounts</h2>
        <p class="mt-1 text-sm text-stone-500">Link or unlink third-party accounts for easier sign-in</p>
      </div>

      @if (loading()) {
        <div class="flex flex-col items-center py-16">
          <div class="animate-spin mb-4 h-8 w-8 rounded-full border border-stone-200 border-t-stone-600"></div>
          <p class="text-sm text-stone-500">Loading…</p>
        </div>
      } @else {
        <div class="space-y-3">
          @for (provider of providers; track provider) {
            @let linked = getLinked(provider);
            <div
              class="flex items-center justify-between rounded-lg border p-4 transition-colors"
              [class]="linked
                ? 'border-stone-200'
                : 'border-stone-200'"
            >
              <div class="flex items-center gap-4">
                <div
                  class="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg border"
                  [class]="linked ? 'border-stone-200 bg-white' : 'border-stone-200 bg-white'"
                >
                  @switch (provider) {
                    @case ('GOOGLE') {
                      <svg class="h-5 w-5" viewBox="0 0 24 24">
                        <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4"/>
                        <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
                        <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z" fill="#FBBC05"/>
                        <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/>
                      </svg>
                    }
                    @case ('GITHUB') {
                      <svg class="h-5 w-5" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M12 .297c-6.63 0-12 5.373-12 12 0 5.303 3.438 9.8 8.205 11.385.6.113.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.042-1.61-4.042-1.61C4.422 18.07 3.633 17.7 3.633 17.7c-1.087-.744.084-.729.084-.729 1.205.084 1.838 1.236 1.838 1.236 1.07 1.835 2.809 1.305 3.495.998.108-.776.417-1.305.76-1.605-2.665-.3-5.466-1.332-5.466-5.93 0-1.31.465-2.38 1.235-3.22-.135-.303-.54-1.523.105-3.176 0 0 1.005-.322 3.3 1.23.96-.267 1.98-.399 3-.405 1.02.006 2.04.138 3 .405 2.28-1.552 3.285-1.23 3.285-1.23.645 1.653.24 2.873.12 3.176.765.84 1.23 1.91 1.23 3.22 0 4.61-2.805 5.625-5.475 5.92.42.36.81 1.096.81 2.22 0 1.606-.015 2.896-.015 3.286 0 .315.21.69.825.57C20.565 22.092 24 17.592 24 12.297c0-6.627-5.373-12-12-12"/>
                      </svg>
                    }
                    @case ('FACEBOOK') {
                      <svg class="h-5 w-5" viewBox="0 0 24 24">
                        <path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z" fill="#1877F2"/>
                      </svg>
                    }
                  }
                </div>
                <div>
                  <p class="text-sm font-medium text-stone-900">{{ providerName(provider) }}</p>
                  @if (linked) {
                    <p class="mt-0.5 text-xs text-stone-500">
                      Linked {{ linked.linkedAt | date: 'MMM d, y' }}
                    </p>
                  } @else {
                    <p class="mt-0.5 text-xs text-stone-400">Not connected</p>
                  }
                </div>
              </div>

              @if (linked) {
                @if (linked.canUnlink) {
                  <button
                    (click)="unlinkProvider(provider)"
                    [disabled]="unlinking() === provider"
                    class="shrink-0 rounded-lg border border-stone-200 px-3.5 py-2 text-xs font-medium text-stone-600 transition-colors hover:border-red-200 hover:bg-red-50 hover:text-red-600 disabled:opacity-50"
                  >
                    @if (unlinking() === provider) {
                      <div class="animate-spin h-3.5 w-3.5 rounded-full border-2 border-stone-300 border-t-stone-600"></div>
                    } @else {
                      Unlink
                    }
                  </button>
                } @else {
                  <span class="shrink-0 rounded-lg bg-stone-100 px-3.5 py-2 text-xs font-medium text-stone-400">
                    Primary
                  </span>
                }
              } @else {
                <button
                  (click)="linkProvider(provider)"
                  [disabled]="!!linking()"
                  class="shrink-0 rounded-lg bg-coral-600 px-3.5 py-2 text-xs font-medium text-white transition-colors hover:bg-coral-700 disabled:opacity-50"
                >
                  @if (linking() === provider) {
                    <div class="animate-spin h-3.5 w-3.5 rounded-full border-2 border-coral-300 border-t-coral-600"></div>
                  } @else {
                    Link
                  }
                </button>
              }
            </div>
          }
        </div>
      }
    </div>
  `,
})
export class ConnectedAccounts implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly apiUrl = environment.apiUrl;

  protected readonly loading = signal(true);
  protected readonly linking = signal<string | null>(null);
  protected readonly unlinking = signal<string | null>(null);
  protected readonly linkedProviders = signal<LinkedProviderResponse[]>([]);

  protected readonly providers = PROVIDERS;

  ngOnInit(): void {
    this.loadProviders();
  }

  private loadProviders(): void {
    this.http
      .get<ApiResponse<LinkedProviderResponse[]>>(`${this.apiUrl}/api/profile/me/providers`)
      .subscribe({
        next: (res) => {
          this.loading.set(false);
          if (res.data) this.linkedProviders.set(res.data);
        },
        error: () => {
          this.loading.set(false);
          this.toast.error('Failed to load connected accounts.');
        },
      });
  }

  getLinked(providerKey: string): LinkedProviderResponse | undefined {
    return this.linkedProviders().find((p) => p.provider === providerKey);
  }

  providerName(key: string): string {
    switch (key) {
      case 'GOOGLE': return 'Google';
      case 'GITHUB': return 'GitHub';
      case 'FACEBOOK': return 'Facebook';
      default: return key;
    }
  }

  linkProvider(provider: string): void {
    this.linking.set(provider);

    sessionStorage.setItem('oauth_provider', provider);
    sessionStorage.setItem('oauth_mode', 'link');

    this.authService.getOAuthUrl(provider).subscribe({
      next: (res) => {
        if (res.data?.url) {
          window.location.href = res.data.url;
        }
      },
      error: (err) => {
        this.linking.set(null);
        sessionStorage.removeItem('oauth_provider');
        sessionStorage.removeItem('oauth_mode');
        const apiErr = err.error as ApiError | undefined;
        this.toast.error(apiErr?.message ?? 'Failed to initiate linking.');
      },
    });
  }

  unlinkProvider(provider: string): void {
    this.unlinking.set(provider);
    this.http
      .delete<ApiResponse<void>>(`${this.apiUrl}/api/auth/oauth/${provider}`)
      .subscribe({
        next: () => {
          this.unlinking.set(null);
          this.linkedProviders.update((list) => list.filter((p) => p.provider !== provider));
          this.toast.success(`${this.providerName(provider)} account unlinked.`);
        },
        error: (err) => {
          this.unlinking.set(null);
          const apiErr = err.error as ApiError | undefined;
          this.toast.error(apiErr?.message ?? 'Failed to unlink account.');
        },
      });
  }
}

