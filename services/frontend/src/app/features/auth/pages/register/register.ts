import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { FormField } from '../../../../shared/components/form-field/form-field';
import { OAuthButtons } from '../../../../shared/components/oauth-buttons/oauth-buttons';
import { ApiError } from '../../../../core/models/api-response.model';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink, FormField, OAuthButtons],
  template: `
    <div class="flex min-h-screen">
      <!-- Left: Form -->
      <div class="flex w-full flex-col bg-white px-8 py-10 lg:w-[45%] lg:px-16">
        <a routerLink="/" class="flex items-center">
          <span class="text-xl font-bold tracking-tight text-stone-900">reveria</span>
        </a>

        <div class="flex flex-1 flex-col justify-center py-12">
          <div class="w-full max-w-sm">
            <h1 class="text-2xl font-bold text-stone-900">Create your account</h1>
            <p class="mt-1.5 text-sm text-stone-500">Join Reveria and start building your world</p>

            <form [formGroup]="form" (ngSubmit)="onSubmit()" class="mt-8">
              <app-form-field label="Email" fieldId="email" [control]="form.controls.email">
                <input
                  id="email"
                  formControlName="email"
                  type="email"
                  class="w-full rounded-lg border border-stone-200 bg-white px-4 py-3 text-sm text-stone-900 transition-colors placeholder:text-stone-400 focus:border-stone-400 focus:outline-none"
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
                  class="w-full rounded-lg border border-stone-200 bg-white px-4 py-3 text-sm text-stone-900 transition-colors placeholder:text-stone-400 focus:border-stone-400 focus:outline-none"
                  placeholder="your_username"
                />
              </app-form-field>

              <app-form-field label="Display Name" fieldId="displayName" [control]="form.controls.displayName">
                <input
                  id="displayName"
                  formControlName="displayName"
                  type="text"
                  class="w-full rounded-lg border border-stone-200 bg-white px-4 py-3 text-sm text-stone-900 transition-colors placeholder:text-stone-400 focus:border-stone-400 focus:outline-none"
                  placeholder="John Doe"
                />
              </app-form-field>

              <app-form-field label="Password" fieldId="password" [control]="form.controls.password">
                <input
                  id="password"
                  formControlName="password"
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
                Create account
              </button>
            </form>

            <app-oauth-buttons />

            <p class="mt-6 text-center text-sm text-stone-500">
              Already have an account?
              <a routerLink="/auth/login" class="ml-1 font-semibold text-coral-600 hover:text-coral-700">
                Log in
              </a>
            </p>
          </div>
        </div>
      </div>

      <!-- Right: Brand panel -->
      <div class="hidden flex-col justify-between bg-stone-950 px-16 py-20 lg:flex lg:w-[55%]">
        <div class="flex items-center">
          <span class="text-xl font-bold tracking-tight text-white">reveria</span>
        </div>

        <div class="max-w-md">
          <h2 class="text-4xl font-bold leading-tight text-white">
            Where imagination becomes reality
          </h2>
          <p class="mt-5 text-base leading-relaxed text-stone-400">
            Join a community of creators building worlds, sharing stories, and connecting with people who see things the way you do.
          </p>
          <div class="mt-10 space-y-3 text-sm text-stone-500">
            <p>— Create and share your own worlds</p>
            <p>— Connect with like-minded creators</p>
            <p>— Build your creative presence</p>
          </div>
        </div>

        <p class="text-xs text-stone-600">© 2025 Reveria. All rights reserved.</p>
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
