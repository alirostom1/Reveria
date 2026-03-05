import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../../../core/services/auth.service';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { ApiError, ApiResponse } from '../../../../core/models/api-response.model';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-oauth-callback',
  template: `
    <div class="flex min-h-screen items-center justify-center bg-white">
      <div class="flex flex-col items-center gap-5 text-center">
        <div class="flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-coral-500 to-coral-600 shadow-lg shadow-coral-500/25">
          <div class="h-7 w-7 animate-spin rounded-full border-3 border-white/30 border-t-white"></div>
        </div>
        <div>
          <p class="text-lg font-semibold text-stone-900">Signing you in...</p>
          <p class="mt-1 text-sm text-stone-500">Please wait while we complete authentication</p>
        </div>
      </div>
    </div>
  `,
})
export class OAuthCallback implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  ngOnInit(): void {
    const code = this.route.snapshot.queryParamMap.get('code');
    const error = this.route.snapshot.queryParamMap.get('error');
    const provider = sessionStorage.getItem('oauth_provider');
    const mode = sessionStorage.getItem('oauth_mode') || 'login';

    // Clean up
    sessionStorage.removeItem('oauth_provider');
    sessionStorage.removeItem('oauth_mode');
    const returnUrl = sessionStorage.getItem('oauth_return_url') ?? '/';
    sessionStorage.removeItem('oauth_return_url');

    if (error || !code || !provider) {
      this.toast.error('Sign-in was cancelled or failed. Please try again.');
      this.router.navigateByUrl(mode === 'link' ? '/profile/settings/accounts' : '/auth/login');
      return;
    }

    const redirectUri = `${window.location.origin}/auth/callback`;

    if (mode === 'link') {
      this.http
        .post<ApiResponse<void>>(`${environment.apiUrl}/api/auth/oauth/link`, {
          provider,
          code,
          redirectUri,
        })
        .subscribe({
          next: () => {
            this.toast.success(`${provider} account linked successfully!`);
            this.router.navigateByUrl('/profile/settings/accounts');
          },
          error: (err) => {
            const apiErr = err.error as ApiError | undefined;
            this.toast.error(apiErr?.message ?? 'Failed to link account. Please try again.');
            this.router.navigateByUrl('/profile/settings/accounts');
          },
        });
    } else {
      this.authService.oauthLogin({ provider, code, redirectUri }).subscribe({
        next: () => {
          this.toast.success('Signed in successfully!');
          this.router.navigateByUrl(returnUrl);
        },
        error: (err) => {
          const apiErr = err.error as ApiError | undefined;
          this.toast.error(apiErr?.message ?? 'Sign-in failed. Please try again.');
          this.router.navigateByUrl('/auth/login');
        },
      });
    }
  }
}
