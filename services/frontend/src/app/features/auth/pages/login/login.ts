import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { FormField } from '../../../../shared/components/form-field/form-field';
import { OAuthButtons } from '../../../../shared/components/oauth-buttons/oauth-buttons';
import { ApiError } from '../../../../core/models/api-response.model';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, FormField, OAuthButtons],
  template: `
    <div class="flex min-h-screen">
      <!-- Left: Form -->
      <div class="flex w-full flex-col bg-white px-8 py-10 lg:w-[30%] lg:px-16">
        <a routerLink="/" class="flex items-center">
          <span class="text-xl font-bold tracking-tight text-stone-900">reveria</span>
        </a>

        <div class="flex flex-1 flex-col justify-center py-12">
          <div class="w-full max-w-sm">
            <h1 class="text-2xl font-bold text-stone-900">Welcome back</h1>
            <p class="mt-1.5 text-sm text-stone-500">Sign in to continue</p>

            <form [formGroup]="form" (ngSubmit)="onSubmit()" class="mt-8">
              <app-form-field label="Email or Username" fieldId="identifier" [control]="form.controls.identifier">
                <input
                  id="identifier"
                  formControlName="identifier"
                  type="text"
                  class="w-full rounded-lg border border-stone-200 bg-white px-4 py-3 text-sm text-stone-900 transition-colors placeholder:text-stone-400 focus:border-stone-400 focus:outline-none"
                  placeholder="you@example.com"
                />
              </app-form-field>

              <app-form-field label="Password" fieldId="password" [control]="form.controls.password">
                <input
                  id="password"
                  formControlName="password"
                  type="password"
                  class="w-full rounded-lg border border-stone-200 bg-white px-4 py-3 text-sm text-stone-900 transition-colors placeholder:text-stone-400 focus:border-stone-400 focus:outline-none"
                  placeholder="Your password"
                />
              </app-form-field>

              <div class="mb-6 flex items-center justify-between">
                <label class="flex cursor-pointer items-center gap-2 text-sm text-stone-600">
                  <input
                    type="checkbox"
                    formControlName="rememberMe"
                    class="rounded border-stone-300 text-coral-600"
                  />
                  Remember me
                </label>
                <a routerLink="/auth/forgot-password" class="text-sm font-medium text-coral-600 hover:text-coral-700">
                  Forgot password?
                </a>
              </div>

              <button
                type="submit"
                [disabled]="submitting()"
                class="flex w-full items-center justify-center gap-2 rounded-lg bg-coral-600 px-4 py-3 text-sm font-semibold text-white transition-colors hover:bg-coral-700 disabled:cursor-not-allowed disabled:opacity-50"
              >
                @if (submitting()) {
                  <div class="h-4 w-4 animate-spin rounded-full border-2 border-white/40 border-t-white"></div>
                }
                Log in
              </button>
            </form>

            <app-oauth-buttons [returnUrl]="returnUrl" />

            <p class="mt-6 text-center text-sm text-stone-500">
              Don't have an account?
              <a routerLink="/auth/register" class="ml-1 font-semibold text-coral-600 hover:text-coral-700">
                Create one
              </a>
            </p>
          </div>
        </div>
      </div>

      <!-- Right: Brand panel -->
      <div class="hidden flex-col justify-between bg-stone-950 pl-180 pr-16 py-20 lg:flex lg:w-[70%]">
        <div></div>
        <div class="max-w-md">
          <h2 class="text-4xl font-bold leading-tight text-white">
            Your worlds are waiting for you
          </h2>
          <p class="mt-5 text-base leading-relaxed text-stone-400">
            Pick up where you left off. Your stories, communities, and creative spaces are all here, just as you left them.
          </p>
          <div class="mt-10 space-y-3 text-sm text-stone-500">
            <p>— Continue your stories and drafts</p>
            <p>— Reconnect with your communities</p>
            <p>— Explore what's new from creators you follow</p>
          </div>
        </div>

        <p class="text-xs text-stone-600">© 2025 Reveria. All rights reserved.</p>
      </div>
    </div>
  `,
})
export class Login {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly toast = inject(ToastService);

  protected readonly submitting = signal(false);

  protected get returnUrl(): string {
    return this.route.snapshot.queryParams['returnUrl'] ?? '/';
  }

  protected readonly form = this.fb.nonNullable.group({
    identifier: ['', Validators.required],
    password: ['', Validators.required],
    rememberMe: [false],
  });

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.authService.login(this.form.getRawValue()).subscribe({
      next: () => {
        this.toast.success('Welcome back!');
        const returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/';
        this.router.navigateByUrl(returnUrl);
      },
      error: (err) => {
        this.submitting.set(false);
        const apiErr = err.error as ApiError | undefined;
        this.toast.error(apiErr?.message || 'Login failed. Please try again.');
      },
    });
  }
}
