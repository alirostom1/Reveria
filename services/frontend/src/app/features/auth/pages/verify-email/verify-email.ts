import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-verify-email',
  imports: [RouterLink],
  template: `
    <div class="relative flex min-h-[calc(100vh-72px)] items-center justify-center overflow-hidden px-4 py-12">
      <!-- Background -->
      <div class="pointer-events-none absolute inset-0 overflow-hidden">
        <div class="animate-float absolute -top-40 -right-40 h-96 w-96 rounded-full bg-coral-100/60 blur-3xl"></div>
        <div class="animate-float-delay-2 absolute -bottom-40 -left-40 h-96 w-96 rounded-full bg-sage-100/50 blur-3xl"></div>
      </div>

      <div class="animate-fade-up relative z-10 w-full max-w-md">
        @if (verifying()) {
          <!-- Loading -->
          <div class="flex flex-col items-center py-20">
            <div class="animate-spin-slow mb-5 h-10 w-10 rounded-full border-4 border-sand-200 border-t-coral-600"></div>
            <p class="text-base text-stone-500 font-medium">Verifying your email...</p>
          </div>
        } @else if (success()) {
          <!-- Success -->
          <div class="rounded-3xl border-2 border-emerald-200 bg-white p-10 shadow-2xl">
            <div class="flex flex-col items-center text-center">
              <div class="mb-6 flex h-20 w-20 items-center justify-center rounded-2xl bg-emerald-100">
                <svg class="h-10 w-10 text-emerald-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
              <h2 class="mb-3 text-2xl font-bold text-stone-900">Email verified!</h2>
              <p class="mb-8 text-base leading-relaxed text-stone-600">
                Your email has been successfully verified. You're all set.
              </p>
              <a
                routerLink="/"
                class="btn-shine group inline-flex items-center gap-2.5 rounded-xl bg-gradient-to-r from-coral-500 to-coral-600 px-8 py-4 text-base font-bold text-white shadow-lg shadow-coral-500/25 transition-all duration-200 hover:shadow-xl hover:-translate-y-0.5"
              >
                Continue to Reveria
                <svg class="h-5 w-5 transition-transform duration-200 group-hover:translate-x-1" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="3">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M13.5 4.5L21 12m0 0l-7.5 7.5M21 12H3" />
                </svg>
              </a>
            </div>
          </div>
        } @else {
          <!-- Error -->
          <div class="rounded-3xl border-2 border-red-200 bg-white p-10 shadow-2xl">
            <div class="flex flex-col items-center text-center">
              <div class="mb-6 flex h-20 w-20 items-center justify-center rounded-2xl bg-red-100">
                <svg class="h-10 w-10 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
                </svg>
              </div>
              <h2 class="mb-3 text-2xl font-bold text-stone-900">Verification failed</h2>
              <p class="mb-8 text-base leading-relaxed text-stone-600">{{ errorMessage() }}</p>
              <a
                routerLink="/"
                class="inline-flex items-center gap-2 text-base font-bold text-coral-600 transition-colors hover:text-coral-700"
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
