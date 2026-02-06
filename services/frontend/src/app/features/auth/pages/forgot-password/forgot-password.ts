import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { FormField } from '../../../../shared/components/form-field/form-field';

@Component({
  selector: 'app-forgot-password',
  imports: [ReactiveFormsModule, RouterLink, FormField],
  template: `
    <div class="relative flex min-h-[calc(100vh-72px)] items-center justify-center overflow-hidden px-4 py-12">
      <!-- Background -->
      <div class="pointer-events-none absolute inset-0 overflow-hidden">
        <div class="animate-float absolute -top-40 -right-40 h-96 w-96 rounded-full bg-coral-100/60 blur-3xl"></div>
        <div class="animate-float-delay-2 absolute -bottom-40 -left-40 h-96 w-96 rounded-full bg-sage-100/50 blur-3xl"></div>
      </div>

      <div class="animate-fade-up relative z-10 w-full max-w-md">
        <!-- Card Header -->
        <div class="mb-8 text-center">
          <div class="mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-coral-500 to-coral-600 shadow-2xl shadow-coral-500/30">
            <svg class="h-8 w-8 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M21.75 6.75v10.5a2.25 2.25 0 01-2.25 2.25h-15a2.25 2.25 0 01-2.25-2.25V6.75m19.5 0A2.25 2.25 0 0019.5 4.5h-15a2.25 2.25 0 00-2.25 2.25m19.5 0v.243a2.25 2.25 0 01-1.07 1.916l-7.5 4.615a2.25 2.25 0 01-2.36 0L3.32 8.91a2.25 2.25 0 01-1.07-1.916V6.75" />
            </svg>
          </div>
          <h1 class="text-3xl font-bold text-stone-900">Forgot your password?</h1>
          <p class="mt-2 text-base text-stone-500">No worries, we'll send you a reset link</p>
        </div>

        @if (sent()) {
          <!-- Success State -->
          <div class="rounded-3xl border-2 border-emerald-200 bg-white p-8 shadow-2xl">
            <div class="flex flex-col items-center text-center">
              <div class="mb-5 flex h-16 w-16 items-center justify-center rounded-2xl bg-emerald-100">
                <svg class="h-8 w-8 text-emerald-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
              <h2 class="mb-3 text-xl font-bold text-stone-900">Check your email</h2>
              <p class="mb-8 text-base leading-relaxed text-stone-600">
                If an account with that email exists, we've sent a password reset link. Check your inbox and spam folder.
              </p>
              <a
                routerLink="/auth/login"
                class="inline-flex items-center gap-2 text-base font-bold text-coral-600 transition-colors hover:text-coral-700"
              >
                <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 19.5L3 12m0 0l7.5-7.5M3 12h18" />
                </svg>
                Back to login
              </a>
            </div>
          </div>
        } @else {
          <!-- Form -->
          <form
            [formGroup]="form"
            (ngSubmit)="onSubmit()"
            class="rounded-3xl border-2 border-sand-200 bg-white p-8 shadow-2xl"
          >
            <app-form-field label="Email address" fieldId="email" [control]="form.controls.email">
              <input
                id="email"
                formControlName="email"
                type="email"
                class="w-full rounded-xl border-2 border-sand-200 bg-sand-50/50 px-4 py-3 text-sm text-stone-900 transition-all duration-200 placeholder:text-stone-400 focus:border-coral-500 focus:bg-white focus:ring-4 focus:ring-coral-500/10 focus:outline-none"
                placeholder="you@example.com"
              />
            </app-form-field>

            <button
              type="submit"
              [disabled]="submitting()"
              class="btn-shine flex w-full items-center justify-center gap-2.5 rounded-xl bg-gradient-to-r from-coral-500 to-coral-600 px-5 py-3.5 text-sm font-bold text-white shadow-lg shadow-coral-500/25 transition-all duration-200 hover:shadow-xl hover:shadow-coral-500/35 hover:-translate-y-0.5 disabled:opacity-50 disabled:shadow-none disabled:translate-y-0"
            >
              @if (submitting()) {
                <div class="animate-spin-slow h-5 w-5 rounded-full border-2 border-white/30 border-t-white"></div>
                Sending...
              } @else {
                Send reset link
              }
            </button>

            <p class="mt-8 text-center">
              <a
                routerLink="/auth/login"
                class="inline-flex items-center gap-2 text-sm font-bold text-coral-600 transition-colors hover:text-coral-700"
              >
                <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 19.5L3 12m0 0l7.5-7.5M3 12h18" />
                </svg>
                Back to login
              </a>
            </p>
          </form>
        }
      </div>
    </div>
  `,
})
export class ForgotPassword {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly toast = inject(ToastService);

  protected readonly submitting = signal(false);
  protected readonly sent = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
  });

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.authService.forgotPassword(this.form.getRawValue()).subscribe({
      next: () => {
        this.sent.set(true);
      },
      error: () => {
        this.submitting.set(false);
        this.toast.error('Something went wrong. Please try again.');
      },
    });
  }
}
