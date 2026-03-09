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
    <div class="flex min-h-screen items-center justify-center bg-stone-50 px-4 py-12">
      <div class="w-full max-w-sm">
        <div class="mb-8 text-center">
          <a routerLink="/" class="mb-6 inline-flex items-center gap-2.5">
            <div class="flex h-8 w-8 items-center justify-center rounded-md bg-coral-600">
              <span class="text-sm font-bold text-white">R</span>
            </div>
            <span class="text-base font-bold text-stone-900">Reveria</span>
          </a>
        </div>

        @if (validating()) {
          <div class="flex flex-col items-center gap-3 py-16">
            <div class="h-6 w-6 animate-spin rounded-full border-2 border-stone-200 border-t-stone-600"></div>
            <p class="text-sm text-stone-500">Validating your reset link...</p>
          </div>
        } @else if (tokenInvalid()) {
          <div class="overflow-hidden rounded-xl border border-stone-200 bg-white">
            <div class="h-1 bg-red-500"></div>
            <div class="p-8 text-center">
              <p class="font-semibold text-stone-900">Link expired</p>
              <p class="mt-2 text-sm leading-relaxed text-stone-500">
                This reset link is invalid or has expired. Please request a new one.
              </p>
              <a
                routerLink="/auth/forgot-password"
                class="mt-6 inline-block rounded-lg bg-coral-600 px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-coral-700"
              >
                Request new link
              </a>
            </div>
          </div>
        } @else if (resetDone()) {
          <div class="overflow-hidden rounded-xl border border-stone-200 bg-white">
            <div class="h-1 bg-emerald-500"></div>
            <div class="p-8 text-center">
              <p class="font-semibold text-stone-900">Password reset</p>
              <p class="mt-2 text-sm leading-relaxed text-stone-500">
                Your password has been successfully updated. You can now log in with your new password.
              </p>
              <a
                routerLink="/auth/login"
                class="mt-6 inline-block rounded-lg bg-coral-600 px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-coral-700"
              >
                Go to login
              </a>
            </div>
          </div>
        } @else {
          <div class="mb-6 text-center">
            <h1 class="text-2xl font-bold text-stone-900">Set new password</h1>
            <p class="mt-2 text-sm text-stone-500">Choose a strong password for your account</p>
          </div>

          <div class="rounded-xl border border-stone-200 bg-white p-8">
            <form [formGroup]="form" (ngSubmit)="onSubmit()">
              <app-form-field label="New Password" fieldId="newPassword" [control]="form.controls.newPassword">
                <input
                  id="newPassword"
                  formControlName="newPassword"
                  type="password"
                  class="w-full rounded-lg border border-stone-200 bg-white px-4 py-3 text-sm text-stone-900 transition-colors placeholder:text-stone-400 focus:border-stone-400 focus:outline-none"
                  placeholder="Minimum 8 characters"
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
                Reset password
              </button>
            </form>
          </div>
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
