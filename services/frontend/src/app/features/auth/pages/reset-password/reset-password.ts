import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { FormField } from '../../../../shared/components/form-field/form-field';
import { ApiError } from '../../../../core/models/api-response.model';

@Component({
  selector: 'app-reset-password',
  imports: [ReactiveFormsModule, RouterLink, FormField],
  template: `
    <div class="relative flex min-h-[calc(100vh-72px)] items-center justify-center overflow-hidden px-4 py-12">
      <!-- Background -->
      <div class="pointer-events-none absolute inset-0 overflow-hidden">
        <div class="animate-float absolute -top-40 -right-40 h-96 w-96 rounded-full bg-coral-100/60 blur-3xl"></div>
        <div class="animate-float-delay-2 absolute -bottom-40 -left-40 h-96 w-96 rounded-full bg-sage-100/50 blur-3xl"></div>
      </div>

      <div class="animate-fade-up relative z-10 w-full max-w-md">
        @if (validating()) {
          <!-- Loading State -->
          <div class="flex flex-col items-center py-20">
            <div class="animate-spin-slow mb-5 h-10 w-10 rounded-full border-4 border-sand-200 border-t-coral-600"></div>
            <p class="text-base text-stone-500 font-medium">Validating your reset link...</p>
          </div>
        } @else if (tokenInvalid()) {
          <!-- Invalid Token -->
          <div class="rounded-3xl border-2 border-red-200 bg-white p-8 shadow-2xl">
            <div class="flex flex-col items-center text-center">
              <div class="mb-5 flex h-16 w-16 items-center justify-center rounded-2xl bg-red-100">
                <svg class="h-8 w-8 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
                </svg>
              </div>
              <h2 class="mb-3 text-xl font-bold text-stone-900">Link expired</h2>
              <p class="mb-8 text-base leading-relaxed text-stone-600">
                This password reset link is invalid or has expired. Please request a new one.
              </p>
              <a
                routerLink="/auth/forgot-password"
                class="btn-shine inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-coral-500 to-coral-600 px-7 py-3.5 text-sm font-bold text-white shadow-lg shadow-coral-500/25 transition-all duration-200 hover:shadow-xl hover:-translate-y-0.5"
              >
                Request new link
              </a>
            </div>
          </div>
        } @else if (resetDone()) {
          <!-- Success State -->
          <div class="rounded-3xl border-2 border-emerald-200 bg-white p-8 shadow-2xl">
            <div class="flex flex-col items-center text-center">
              <div class="mb-5 flex h-16 w-16 items-center justify-center rounded-2xl bg-emerald-100">
                <svg class="h-8 w-8 text-emerald-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
              <h2 class="mb-3 text-xl font-bold text-stone-900">Password reset!</h2>
              <p class="mb-8 text-base leading-relaxed text-stone-600">
                Your password has been successfully reset. You can now log in with your new password.
              </p>
              <a
                routerLink="/auth/login"
                class="btn-shine inline-flex items-center gap-2.5 rounded-xl bg-gradient-to-r from-coral-500 to-coral-600 px-7 py-3.5 text-sm font-bold text-white shadow-lg shadow-coral-500/25 transition-all duration-200 hover:shadow-xl hover:-translate-y-0.5"
              >
                Go to login
                <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="3">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M13.5 4.5L21 12m0 0l-7.5 7.5M21 12H3" />
                </svg>
              </a>
            </div>
          </div>
        } @else {
          <!-- Form -->
          <div class="mb-8 text-center">
            <div class="mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-coral-500 to-coral-600 shadow-2xl shadow-coral-500/30">
              <svg class="h-8 w-8 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M16.5 10.5V6.75a4.5 4.5 0 10-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 002.25-2.25v-6.75a2.25 2.25 0 00-2.25-2.25H6.75a2.25 2.25 0 00-2.25 2.25v6.75a2.25 2.25 0 002.25 2.25z" />
              </svg>
            </div>
            <h1 class="text-3xl font-bold text-stone-900">Set new password</h1>
            <p class="mt-2 text-base text-stone-500">Choose a strong password for your account</p>
          </div>

          <form
            [formGroup]="form"
            (ngSubmit)="onSubmit()"
            class="rounded-3xl border-2 border-sand-200 bg-white p-8 shadow-2xl"
          >
            <app-form-field label="New Password" fieldId="newPassword" [control]="form.controls.newPassword">
              <input
                id="newPassword"
                formControlName="newPassword"
                type="password"
                class="w-full rounded-xl border-2 border-sand-200 bg-sand-50/50 px-4 py-3 text-sm text-stone-900 transition-all duration-200 placeholder:text-stone-400 focus:border-coral-500 focus:bg-white focus:ring-4 focus:ring-coral-500/10 focus:outline-none"
                placeholder="Minimum 8 characters"
              />
            </app-form-field>

            <button
              type="submit"
              [disabled]="submitting()"
              class="btn-shine flex w-full items-center justify-center gap-2.5 rounded-xl bg-gradient-to-r from-coral-500 to-coral-600 px-5 py-3.5 text-sm font-bold text-white shadow-lg shadow-coral-500/25 transition-all duration-200 hover:shadow-xl hover:shadow-coral-500/35 hover:-translate-y-0.5 disabled:opacity-50 disabled:shadow-none disabled:translate-y-0"
            >
              @if (submitting()) {
                <div class="animate-spin-slow h-5 w-5 rounded-full border-2 border-white/30 border-t-white"></div>
                Resetting...
              } @else {
                Reset password
              }
            </button>
          </form>
        }
      </div>
    </div>
  `,
})
export class ResetPassword implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  protected readonly validating = signal(true);
  protected readonly tokenInvalid = signal(false);
  protected readonly resetDone = signal(false);
  protected readonly submitting = signal(false);
  private token = '';

  protected readonly form = this.fb.nonNullable.group({
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
  });

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParams['token'] || '';
    if (!this.token) {
      this.validating.set(false);
      this.tokenInvalid.set(true);
      return;
    }

    this.authService.validateResetToken(this.token).subscribe({
      next: (res) => {
        this.validating.set(false);
        if (!res.data) {
          this.tokenInvalid.set(true);
        }
      },
      error: () => {
        this.validating.set(false);
        this.tokenInvalid.set(true);
      },
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.authService
      .resetPassword({ token: this.token, newPassword: this.form.getRawValue().newPassword })
      .subscribe({
        next: () => {
          this.resetDone.set(true);
        },
        error: (err) => {
          this.submitting.set(false);
          const apiErr = err.error as ApiError | undefined;
          this.toast.error(apiErr?.message || 'Password reset failed. Please try again.');
        },
      });
  }
}
