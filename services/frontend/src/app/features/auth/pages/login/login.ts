import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { FormField } from '../../../../shared/components/form-field/form-field';
import { ApiError } from '../../../../core/models/api-response.model';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, FormField],
  template: `
    <div class="flex min-h-screen bg-white">
      <!-- Left: Form Side -->
      <div class="flex w-full flex-col px-6 py-8 sm:px-10 lg:w-[45%] lg:px-16 xl:px-20">
        <!-- Brand -->
        <a routerLink="/" class="flex items-center gap-3">
          <div
            class="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-coral-500 to-coral-600 shadow-lg shadow-coral-500/25"
          >
            <svg class="h-5.5 w-5.5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09zM18.259 8.715L18 9.75l-.259-1.035a3.375 3.375 0 00-2.455-2.456L14.25 6l1.036-.259a3.375 3.375 0 002.455-2.456L18 2.25l.259 1.035a3.375 3.375 0 002.455 2.456L21.75 6l-1.036.259a3.375 3.375 0 00-2.455 2.456z"
              />
            </svg>
          </div>
          <span class="text-xl font-bold tracking-tight text-stone-900">Reveria</span>
        </a>

        <!-- Form Content -->
        <div class="flex flex-1 flex-col justify-center py-12">
          <div class="mx-auto w-full max-w-[440px] lg:mx-0">
            <h1 class="text-4xl font-bold tracking-tight text-stone-900">Welcome back</h1>
            <p class="mt-3 text-lg text-stone-500">Sign in to continue your creative journey</p>

            <form [formGroup]="form" (ngSubmit)="onSubmit()" class="mt-10">
              <app-form-field label="Email or Username" fieldId="identifier" [control]="form.controls.identifier">
                <input
                  id="identifier"
                  formControlName="identifier"
                  type="text"
                  class="w-full rounded-xl border-2 border-sand-200 bg-sand-50/50 px-5 py-4 text-base text-stone-900 transition-all duration-200 placeholder:text-stone-400 focus:border-coral-500 focus:bg-white focus:ring-4 focus:ring-coral-500/10 focus:outline-none"
                  placeholder="you@example.com"
                />
              </app-form-field>

              <app-form-field label="Password" fieldId="password" [control]="form.controls.password">
                <input
                  id="password"
                  formControlName="password"
                  type="password"
                  class="w-full rounded-xl border-2 border-sand-200 bg-sand-50/50 px-5 py-4 text-base text-stone-900 transition-all duration-200 placeholder:text-stone-400 focus:border-coral-500 focus:bg-white focus:ring-4 focus:ring-coral-500/10 focus:outline-none"
                  placeholder="Enter your password"
                />
              </app-form-field>

              <div class="mb-8 flex items-center justify-between">
                <label class="flex cursor-pointer items-center gap-2.5 text-sm font-medium text-stone-600">
                  <input
                    type="checkbox"
                    formControlName="rememberMe"
                    class="h-4.5 w-4.5 rounded-md border-2 border-sand-300 text-coral-600 focus:ring-coral-500/20"
                  />
                  Remember me
                </label>
                <a
                  routerLink="/auth/forgot-password"
                  class="text-sm font-semibold text-coral-600 transition-colors hover:text-coral-700"
                >
                  Forgot password?
                </a>
              </div>

              <button
                type="submit"
                [disabled]="submitting()"
                class="btn-shine flex w-full items-center justify-center gap-2.5 rounded-xl bg-gradient-to-r from-coral-500 to-coral-600 px-6 py-4 text-base font-bold text-white shadow-lg shadow-coral-500/25 transition-all duration-200 hover:shadow-xl hover:shadow-coral-500/35 hover:-translate-y-0.5 disabled:opacity-50 disabled:shadow-none disabled:translate-y-0"
              >
                @if (submitting()) {
                  <div class="animate-spin-slow h-5 w-5 rounded-full border-2 border-white/30 border-t-white"></div>
                  Logging in...
                } @else {
                  Log in
                }
              </button>
            </form>

            <p class="mt-10 text-center text-base text-stone-500">
              Don't have an account?
              <a routerLink="/auth/register" class="ml-1 font-bold text-coral-600 transition-colors hover:text-coral-700">
                Create one
              </a>
            </p>
          </div>
        </div>
      </div>

      <!-- Right: Motivational Panel -->
      <div class="relative hidden overflow-hidden bg-stone-900 lg:flex lg:w-[55%] lg:flex-col lg:justify-center lg:p-16 xl:p-20">
        <!-- Decorative gradient orbs -->
        <div class="animate-float absolute -top-32 -right-32 h-[500px] w-[500px] rounded-full bg-coral-500/15 blur-3xl"></div>
        <div class="animate-float-delay-2 absolute -bottom-32 -left-32 h-96 w-96 rounded-full bg-sage-500/10 blur-3xl"></div>
        <div class="animate-float-delay-1 absolute right-1/4 top-1/3 h-48 w-48 rounded-full bg-coral-600/10 blur-2xl"></div>

        <!-- Content -->
        <div class="animate-fade-up relative z-10 max-w-xl">
          <!-- Badge -->
          <div
            class="mb-8 inline-flex items-center gap-2.5 rounded-full border border-white/10 bg-white/5 px-5 py-2.5 text-sm font-medium text-coral-400 backdrop-blur-sm"
          >
            <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09z"
              />
            </svg>
            Welcome back, creator
          </div>

          <!-- Headline -->
          <h2 class="text-5xl font-bold leading-[1.15] text-white">
            Your worlds are<br />
            <span class="bg-gradient-to-r from-coral-400 to-sage-400 bg-clip-text text-transparent">waiting for you</span>
          </h2>

          <p class="mt-6 text-lg leading-relaxed text-stone-400">
            Pick up where you left off. Your stories, your communities, and your creative spaces are all here — just as you
            left them.
          </p>

          <!-- Feature cards -->
          <div class="mt-12 space-y-4">
            <div class="flex items-center gap-4 rounded-2xl border border-white/[0.06] bg-white/[0.03] p-5 backdrop-blur-sm">
              <div class="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-coral-500/15">
                <svg class="h-6 w-6 text-coral-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    d="M12 6.042A8.967 8.967 0 006 3.75c-1.052 0-2.062.18-3 .512v14.25A8.987 8.987 0 016 18c2.305 0 4.408.867 6 2.292m0-14.25a8.966 8.966 0 016-2.292c1.052 0 2.062.18 3 .512v14.25A8.987 8.987 0 0018 18a8.967 8.967 0 00-6 2.292m0-14.25v14.25"
                  />
                </svg>
              </div>
              <div>
                <h3 class="font-semibold text-white">Continue your stories</h3>
                <p class="mt-0.5 text-sm text-stone-400">Your drafts and narratives await your return</p>
              </div>
            </div>

            <div class="flex items-center gap-4 rounded-2xl border border-white/[0.06] bg-white/[0.03] p-5 backdrop-blur-sm">
              <div class="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-sage-500/15">
                <svg class="h-6 w-6 text-sage-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    d="M18 18.72a9.094 9.094 0 003.741-.479 3 3 0 00-4.682-2.72m.94 3.198l.001.031c0 .225-.012.447-.037.666A11.944 11.944 0 0112 21c-2.17 0-4.207-.576-5.963-1.584A6.062 6.062 0 016 18.719m12 0a5.971 5.971 0 00-.941-3.197m0 0A5.995 5.995 0 0012 12.75a5.995 5.995 0 00-5.058 2.772m0 0a3 3 0 00-4.681 2.72 8.986 8.986 0 003.74.477m.94-3.197a5.971 5.971 0 00-.94 3.197M15 6.75a3 3 0 11-6 0 3 3 0 016 0zm6 3a2.25 2.25 0 11-4.5 0 2.25 2.25 0 014.5 0zm-13.5 0a2.25 2.25 0 11-4.5 0 2.25 2.25 0 014.5 0z"
                  />
                </svg>
              </div>
              <div>
                <h3 class="font-semibold text-white">Reconnect with friends</h3>
                <p class="mt-0.5 text-sm text-stone-400">See what your community has been up to</p>
              </div>
            </div>

            <div class="flex items-center gap-4 rounded-2xl border border-white/[0.06] bg-white/[0.03] p-5 backdrop-blur-sm">
              <div class="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-sand-500/15">
                <svg class="h-6 w-6 text-sand-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    d="M9.53 16.122a3 3 0 00-5.78 1.128 2.25 2.25 0 01-2.4 2.245 4.5 4.5 0 008.4-2.245c0-.399-.078-.78-.22-1.128zm0 0a15.998 15.998 0 003.388-1.62m-5.043-.025a15.994 15.994 0 011.622-3.395m3.42 3.42a15.995 15.995 0 004.764-4.648l3.876-5.814a1.151 1.151 0 00-1.597-1.597L14.146 6.32a15.996 15.996 0 00-4.649 4.763m3.42 3.42a6.776 6.776 0 00-3.42-3.42"
                  />
                </svg>
              </div>
              <div>
                <h3 class="font-semibold text-white">Explore new creations</h3>
                <p class="mt-0.5 text-sm text-stone-400">Discover fresh works from creators you follow</p>
              </div>
            </div>
          </div>
        </div>
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
