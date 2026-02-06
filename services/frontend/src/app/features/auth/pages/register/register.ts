import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { FormField } from '../../../../shared/components/form-field/form-field';
import { ApiError } from '../../../../core/models/api-response.model';

@Component({
  selector: 'app-register',
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
            <h1 class="text-4xl font-bold tracking-tight text-stone-900">Create your account</h1>
            <p class="mt-3 text-lg text-stone-500">Join Reveria and start building your world</p>

            <form [formGroup]="form" (ngSubmit)="onSubmit()" class="mt-10">
              <app-form-field label="Email" fieldId="email" [control]="form.controls.email">
                <input
                  id="email"
                  formControlName="email"
                  type="email"
                  class="w-full rounded-xl border-2 border-sand-200 bg-sand-50/50 px-5 py-4 text-base text-stone-900 transition-all duration-200 placeholder:text-stone-400 focus:border-coral-500 focus:bg-white focus:ring-4 focus:ring-coral-500/10 focus:outline-none"
                  placeholder="you@example.com"
                />
              </app-form-field>

              <app-form-field
                label="Username"
                fieldId="username"
                [control]="form.controls.username"
                patternMessage="Only letters, numbers, and underscores allowed"
              >
                <input
                  id="username"
                  formControlName="username"
                  type="text"
                  class="w-full rounded-xl border-2 border-sand-200 bg-sand-50/50 px-5 py-4 text-base text-stone-900 transition-all duration-200 placeholder:text-stone-400 focus:border-coral-500 focus:bg-white focus:ring-4 focus:ring-coral-500/10 focus:outline-none"
                  placeholder="your_username"
                />
              </app-form-field>

              <app-form-field label="Display Name" fieldId="displayName" [control]="form.controls.displayName">
                <input
                  id="displayName"
                  formControlName="displayName"
                  type="text"
                  class="w-full rounded-xl border-2 border-sand-200 bg-sand-50/50 px-5 py-4 text-base text-stone-900 transition-all duration-200 placeholder:text-stone-400 focus:border-coral-500 focus:bg-white focus:ring-4 focus:ring-coral-500/10 focus:outline-none"
                  placeholder="John Doe"
                />
              </app-form-field>

              <app-form-field label="Password" fieldId="password" [control]="form.controls.password">
                <input
                  id="password"
                  formControlName="password"
                  type="password"
                  class="w-full rounded-xl border-2 border-sand-200 bg-sand-50/50 px-5 py-4 text-base text-stone-900 transition-all duration-200 placeholder:text-stone-400 focus:border-coral-500 focus:bg-white focus:ring-4 focus:ring-coral-500/10 focus:outline-none"
                  placeholder="Minimum 8 characters"
                />
              </app-form-field>

              <button
                type="submit"
                [disabled]="submitting()"
                class="btn-shine mt-3 flex w-full items-center justify-center gap-2.5 rounded-xl bg-gradient-to-r from-coral-500 to-coral-600 px-6 py-4 text-base font-bold text-white shadow-lg shadow-coral-500/25 transition-all duration-200 hover:shadow-xl hover:shadow-coral-500/35 hover:-translate-y-0.5 disabled:opacity-50 disabled:shadow-none disabled:translate-y-0"
              >
                @if (submitting()) {
                  <div class="animate-spin-slow h-5 w-5 rounded-full border-2 border-white/30 border-t-white"></div>
                  Creating account...
                } @else {
                  Create account
                }
              </button>
            </form>

            <p class="mt-10 text-center text-base text-stone-500">
              Already have an account?
              <a routerLink="/auth/login" class="ml-1 font-bold text-coral-600 transition-colors hover:text-coral-700">
                Log in
              </a>
            </p>
          </div>
        </div>
      </div>

      <!-- Right: Motivational Panel -->
      <div class="relative hidden overflow-hidden bg-stone-900 lg:flex lg:w-[55%] lg:flex-col lg:justify-center lg:p-16 xl:p-20">
        <!-- Decorative gradient orbs -->
        <div class="animate-float absolute -top-24 -left-24 h-[450px] w-[450px] rounded-full bg-sage-500/15 blur-3xl"></div>
        <div class="animate-float-delay-1 absolute -bottom-32 -right-32 h-96 w-96 rounded-full bg-coral-500/10 blur-3xl"></div>
        <div class="animate-float-delay-2 absolute left-1/3 top-1/4 h-56 w-56 rounded-full bg-sand-500/10 blur-2xl"></div>

        <!-- Content -->
        <div class="animate-fade-up relative z-10 max-w-xl">
          <!-- Badge -->
          <div
            class="mb-8 inline-flex items-center gap-2.5 rounded-full border border-white/10 bg-white/5 px-5 py-2.5 text-sm font-medium text-sage-400 backdrop-blur-sm"
          >
            <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M12 21a9.004 9.004 0 008.716-6.747M12 21a9.004 9.004 0 01-8.716-6.747M12 21c2.485 0 4.5-4.03 4.5-9S14.485 3 12 3m0 18c-2.485 0-4.5-4.03-4.5-9S9.515 3 12 3m0 0a8.997 8.997 0 017.843 4.582M12 3a8.997 8.997 0 00-7.843 4.582m15.686 0A11.953 11.953 0 0112 10.5c-2.998 0-5.74-1.1-7.843-2.918m15.686 0A8.959 8.959 0 0121 12c0 .778-.099 1.533-.284 2.253m0 0A17.919 17.919 0 0112 16.5c-3.162 0-6.133-.815-8.716-2.247m0 0A9.015 9.015 0 013 12c0-1.605.42-3.113 1.157-4.418"
              />
            </svg>
            Join the community
          </div>

          <!-- Headline -->
          <h2 class="text-5xl font-bold leading-[1.15] text-white">
            Where imagination<br />
            <span class="bg-gradient-to-r from-sage-400 to-coral-400 bg-clip-text text-transparent">becomes reality</span>
          </h2>

          <p class="mt-6 text-lg leading-relaxed text-stone-400">
            Join a vibrant community of dreamers and creators. Build worlds, share stories, and connect with people who see
            things the way you do.
          </p>

          <!-- Feature cards -->
          <div class="mt-12 space-y-4">
            <div class="flex items-center gap-4 rounded-2xl border border-white/[0.06] bg-white/[0.03] p-5 backdrop-blur-sm">
              <div class="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-sage-500/15">
                <svg class="h-6 w-6 text-sage-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    d="M12 21v-8.25M15.75 21v-8.25M8.25 21v-8.25M3 9l9-6 9 6m-1.5 12V10.332A48.36 48.36 0 0012 9.75c-2.551 0-5.056.2-7.5.582V21M3 21h18M12 6.75h.008v.008H12V6.75z"
                  />
                </svg>
              </div>
              <div>
                <h3 class="font-semibold text-white">Create your world</h3>
                <p class="mt-0.5 text-sm text-stone-400">Build unique stories and share them with the community</p>
              </div>
            </div>

            <div class="flex items-center gap-4 rounded-2xl border border-white/[0.06] bg-white/[0.03] p-5 backdrop-blur-sm">
              <div class="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-coral-500/15">
                <svg class="h-6 w-6 text-coral-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12z"
                  />
                </svg>
              </div>
              <div>
                <h3 class="font-semibold text-white">Find your people</h3>
                <p class="mt-0.5 text-sm text-stone-400">Connect with like-minded creators and friends</p>
              </div>
            </div>

            <div class="flex items-center gap-4 rounded-2xl border border-white/[0.06] bg-white/[0.03] p-5 backdrop-blur-sm">
              <div class="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-sand-500/15">
                <svg class="h-6 w-6 text-sand-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    d="M15.59 14.37a6 6 0 01-5.84 7.38v-4.8m5.84-2.58a14.98 14.98 0 006.16-12.12A14.98 14.98 0 009.631 8.41m5.96 5.96a14.926 14.926 0 01-5.841 2.58m-.119-8.54a6 6 0 00-7.381 5.84h4.8m2.58-5.84a14.927 14.927 0 00-2.58 5.84m2.699 2.7c-.103.021-.207.041-.311.06a15.09 15.09 0 01-2.448-2.448 14.9 14.9 0 01.06-.312m-2.24 2.39a4.493 4.493 0 00-1.757 4.306 4.493 4.493 0 004.306-1.758M16.5 9a1.5 1.5 0 11-3 0 1.5 1.5 0 013 0z"
                  />
                </svg>
              </div>
              <div>
                <h3 class="font-semibold text-white">Express yourself</h3>
                <p class="mt-0.5 text-sm text-stone-400">Customize your profile and creative spaces</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class Register {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  protected readonly submitting = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(30), Validators.pattern(/^\w+$/)]],
    displayName: ['', Validators.maxLength(50)],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.authService.register(this.form.getRawValue()).subscribe({
      next: () => {
        this.toast.success('Account created successfully!');
        this.router.navigateByUrl('/');
      },
      error: (err) => {
        this.submitting.set(false);
        const apiErr = err.error as ApiError | undefined;
        this.toast.error(apiErr?.message || 'Registration failed. Please try again.');
      },
    });
  }
}
