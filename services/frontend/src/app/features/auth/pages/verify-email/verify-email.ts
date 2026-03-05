import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-verify-email',
  imports: [RouterLink],
  template: `
    <div class="flex min-h-screen items-center justify-center bg-stone-50 px-4 py-12">
      <div class="w-full max-w-sm">
        <div class="mb-8 text-center">
          <a routerLink="/" class="inline-flex items-center gap-2.5">
            <div class="flex h-8 w-8 items-center justify-center rounded-md bg-coral-600">
              <span class="text-sm font-bold text-white">R</span>
            </div>
            <span class="text-base font-bold text-stone-900">Reveria</span>
          </a>
        </div>

        @if (verifying()) {
          <div class="flex flex-col items-center gap-3 py-16">
            <div class="h-6 w-6 animate-spin rounded-full border-2 border-stone-200 border-t-stone-600"></div>
            <p class="text-sm text-stone-500">Verifying your email...</p>
          </div>
        } @else if (success()) {
          <div class="overflow-hidden rounded-xl border border-stone-200 bg-white">
            <div class="h-1 bg-emerald-500"></div>
            <div class="p-8 text-center">
              <p class="font-semibold text-stone-900">Email verified</p>
              <p class="mt-2 text-sm leading-relaxed text-stone-500">
                Your email has been verified. You're all set to use Reveria.
              </p>
              <a
                routerLink="/"
                class="mt-6 inline-block rounded-lg bg-coral-600 px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-coral-700"
              >
                Continue to Reveria
              </a>
            </div>
          </div>
        } @else {
          <div class="overflow-hidden rounded-xl border border-stone-200 bg-white">
            <div class="h-1 bg-red-500"></div>
            <div class="p-8 text-center">
              <p class="font-semibold text-stone-900">Verification failed</p>
              <p class="mt-2 text-sm leading-relaxed text-stone-500">{{ errorMessage() }}</p>
              <a
                routerLink="/"
                class="mt-6 inline-block text-sm font-medium text-coral-600 hover:text-coral-700"
              >
                Go to home
              </a>
            </div>
          </div>
        }
      </div>
    </div>
  `,
})
export class VerifyEmail implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly route = inject(ActivatedRoute);

  protected readonly verifying = signal(true);
  protected readonly success = signal(false);
  protected readonly errorMessage = signal('The verification link is invalid or has expired.');

  ngOnInit(): void {
    const token = this.route.snapshot.queryParams['token'] || '';
    if (!token) {
      this.verifying.set(false);
      this.errorMessage.set('No verification token was provided.');
      return;
    }

    this.authService.verifyEmail({ token }).subscribe({
      next: () => {
        this.verifying.set(false);
        this.success.set(true);
      },
      error: () => {
        this.verifying.set(false);
      },
    });
  }
}
