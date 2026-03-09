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
    <div class="flex min-h-screen items-center justify-center bg-stone-50 px-4 py-12">
      <div class="w-full max-w-sm">
        <div class="mb-8 text-center">
          <a routerLink="/" class="mb-6 inline-flex items-center gap-2.5">
            <div class="flex h-8 w-8 items-center justify-center rounded-md bg-coral-600">
              <span class="text-sm font-bold text-white">R</span>
            </div>
            <span class="text-base font-bold text-stone-900">Reveria</span>
          </a>
          <h1 class="mt-6 text-2xl font-bold text-stone-900">Forgot your password?</h1>
          <p class="mt-2 text-sm text-stone-500">Enter your email and we'll send a reset link</p>
        </div>

        @if (sent()) {
          <div class="overflow-hidden rounded-xl border border-stone-200 bg-white">
            <div class="h-1 bg-emerald-500"></div>
            <div class="p-8 text-center">
              <p class="font-semibold text-stone-900">Check your inbox</p>
              <p class="mt-2 text-sm leading-relaxed text-stone-500">
                If an account with that email exists, we've sent a reset link. Check your inbox and spam folder.
              </p>
              <a
                routerLink="/auth/login"
                class="mt-6 inline-block text-sm font-medium text-coral-600 hover:text-coral-700"
              >
                ← Back to login
              </a>
            </div>
          </div>
        } @else {
          <div class="rounded-xl border border-stone-200 bg-white p-8">
            <form [formGroup]="form" (ngSubmit)="onSubmit()">
              <app-form-field label="Email address" fieldId="email" [control]="form.controls.email">
                <input
                  id="email"
                  formControlName="email"
                  type="email"
                  class="w-full rounded-lg border border-stone-200 bg-white px-4 py-3 text-sm text-stone-900 transition-colors placeholder:text-stone-400 focus:border-stone-400 focus:outline-none"
                  placeholder="you@example.com"
                />
              </app-form-field>

              <button
                type="submit"
                [disabled]="submitting()"
                class="flex w-full items-center justify-center gap-2 rounded-lg bg-coral-600 px-4 py-3 text-sm font-semibold text-white transition-colors hover:bg-coral-700 disabled:cursor-not-allowed disabled:opacity-50"
              >
                @if (submitting()) {
                  <div class="h-4 w-4 animate-spin rounded-full border-2 border-white/40 border-t-white"></div>
                }
                Send reset link
              </button>
            </form>

            <p class="mt-6 text-center text-sm">
              <a routerLink="/auth/login" class="font-medium text-coral-600 hover:text-coral-700">
                ← Back to login
              </a>
            </p>
          </div>
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
