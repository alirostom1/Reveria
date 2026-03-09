import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { ApiResponse, ApiError } from '../../../../core/models/api-response.model';
import { UserProfileResponse } from '../../../../core/models/profile.model';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { FormField } from '../../../../shared/components/form-field/form-field';

@Component({
  selector: 'app-change-password',
  imports: [ReactiveFormsModule, FormField],
  template: `
    <div class="rounded-lg border border-stone-200 bg-white p-6 sm:p-8">
      <div class="mb-6">
        <h2 class="text-lg font-bold text-stone-900">
          {{ hasPassword() ? 'Change password' : 'Set a password' }}
        </h2>
        <p class="mt-1 text-sm text-stone-500">
          {{ hasPassword()
            ? 'Update your password to keep your account secure'
            : 'Add a password so you can also sign in with your email'
          }}
        </p>
      </div>

      @if (loading()) {
        <div class="flex flex-col items-center py-16">
          <div class="animate-spin mb-4 h-8 w-8 rounded-full border border-stone-200 border-t-stone-600"></div>
          <p class="text-sm text-stone-500">Loading…</p>
        </div>
      } @else {
        @if (!hasPassword()) {
          <div class="mb-6 flex items-start gap-3 rounded-lg border border-blue-200 bg-blue-50/50 p-4">
            <svg class="mt-0.5 h-5 w-5 shrink-0 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M11.25 11.25l.041-.02a.75.75 0 011.063.852l-.708 2.836a.75.75 0 001.063.853l.041-.021M21 12a9 9 0 11-18 0 9 9 0 0118 0zm-9-3.75h.008v.008H12V8.25z" />
            </svg>
            <p class="text-sm text-blue-800">
              You signed up with an external provider. Setting a password lets you also log in with your email and password.
            </p>
          </div>
        }

        <form [formGroup]="form" (ngSubmit)="onSubmit()">
          @if (hasPassword()) {
            <app-form-field label="Current password" fieldId="currentPassword" [control]="form.controls.currentPassword">
              <input
                id="currentPassword"
                formControlName="currentPassword"
                type="password"
                autocomplete="current-password"
                class="w-full rounded-lg border border-stone-200 bg-stone-50/50 px-4 py-3 text-sm text-stone-900 transition-colors placeholder:text-stone-400 focus:border-coral-500 focus:bg-white focus:ring-4 focus:ring-coral-500/10 focus:outline-none"
                placeholder="Enter current password"
              />
            </app-form-field>
          }

          <app-form-field label="New password" fieldId="newPassword" [control]="form.controls.newPassword">
            <input
              id="newPassword"
              formControlName="newPassword"
              type="password"
              autocomplete="new-password"
              class="w-full rounded-lg border border-stone-200 bg-stone-50/50 px-4 py-3 text-sm text-stone-900 transition-colors placeholder:text-stone-400 focus:border-coral-500 focus:bg-white focus:ring-4 focus:ring-coral-500/10 focus:outline-none"
              placeholder="At least 8 characters"
            />
          </app-form-field>

          <app-form-field label="Confirm new password" fieldId="confirmPassword" [control]="form.controls.confirmPassword">
            <input
              id="confirmPassword"
              formControlName="confirmPassword"
              type="password"
              autocomplete="new-password"
              class="w-full rounded-lg border border-stone-200 bg-stone-50/50 px-4 py-3 text-sm text-stone-900 transition-colors placeholder:text-stone-400 focus:border-coral-500 focus:bg-white focus:ring-4 focus:ring-coral-500/10 focus:outline-none"
              placeholder="Confirm new password"
            />
          </app-form-field>

          @if (form.hasError('passwordMismatch') && form.controls.confirmPassword.touched) {
            <p class="-mt-2 mb-4 text-xs text-red-600">Passwords do not match</p>
          }

          <!-- Revoke sessions toggle -->
          <label class="mb-6 flex cursor-pointer items-start gap-3 rounded-lg border border-stone-200 bg-stone-50/50 p-4 transition-colors hover:bg-stone-100/50">
            <input
              type="checkbox"
              formControlName="revokeOtherSessions"
              class="mt-0.5 h-4 w-4 rounded border-stone-300 text-coral-600 focus:ring-coral-600"
            />
            <div>
              <p class="text-sm font-semibold text-stone-800">Log out of other sessions</p>
              <p class="text-xs text-stone-500">Revoke all other active sessions after changing password</p>
            </div>
          </label>

          <button
            type="submit"
            [disabled]="submitting()"
            class="inline-flex items-center justify-center gap-2 rounded-lg bg-coral-600 px-5 py-2.5 text-sm font-medium text-white transition-colors hover:bg-coral-700 disabled:opacity-50"
          >
            @if (submitting()) {
              <div class="animate-spin h-5 w-5 rounded-full border-2 border-white/30 border-t-white"></div>
              {{ hasPassword() ? 'Changing…' : 'Setting…' }}
            } @else {
              {{ hasPassword() ? 'Update password' : 'Set password' }}
            }
          </button>
        </form>
      }
    </div>
  `,
})
export class ChangePassword implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);
  private readonly apiUrl = environment.apiUrl;

  protected readonly loading = signal(true);
  protected readonly submitting = signal(false);
  protected readonly hasPassword = signal(true);

  protected readonly form = this.fb.nonNullable.group(
    {
      currentPassword: [''],
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]],
      revokeOtherSessions: [false],
    },
    {
      validators: [
        (group: any) => {
          const pass = group.get('newPassword')?.value;
          const confirm = group.get('confirmPassword')?.value;
          return pass === confirm ? null : { passwordMismatch: true };
        },
      ],
    }
  );

  ngOnInit(): void {
    this.http
      .get<ApiResponse<UserProfileResponse>>(`${this.apiUrl}/api/profile/me`)
      .subscribe({
        next: (res) => {
          this.loading.set(false);
          if (res.data) {
            this.hasPassword.set(res.data.hasPassword);
          }
        },
        error: () => this.loading.set(false),
      });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    const { currentPassword, newPassword, revokeOtherSessions } = this.form.getRawValue();

    this.http
      .post<ApiResponse<void>>(`${this.apiUrl}/api/auth/change-password`, {
        currentPassword: this.hasPassword() ? currentPassword : null,
        newPassword,
        revokeOtherSessions,
      })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          const msg = this.hasPassword()
            ? 'Password changed successfully!'
            : 'Password set successfully! You can now sign in with your email.';
          this.form.reset();
          this.hasPassword.set(true);
          this.toast.success(msg);
        },
        error: (err) => {
          this.submitting.set(false);
          const apiErr = err.error as ApiError | undefined;
          this.toast.error(apiErr?.message || 'Failed to change password.');
        },
      });
  }
}

