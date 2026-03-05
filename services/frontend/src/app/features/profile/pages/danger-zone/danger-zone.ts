import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { ApiResponse, ApiError } from '../../../../core/models/api-response.model';
import { AuthService } from '../../../../core/services/auth.service';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { FormField } from '../../../../shared/components/form-field/form-field';

@Component({
  selector: 'app-danger-zone',
  imports: [ReactiveFormsModule, FormField],
  template: `
    <div class="rounded-lg border border-red-200 bg-white p-6 sm:p-8">
      <div class="mb-6">
        <h2 class="text-lg font-bold text-red-700">Danger zone</h2>
        <p class="mt-1 text-sm text-stone-500">Irreversible actions that affect your account</p>
      </div>

      <!-- Deactivate Account -->
      <div class="rounded-lg border border-red-200 bg-red-50/50 p-5">
        <div class="flex items-start gap-4">
          <div class="flex h-11 w-11 shrink-0 items-center justify-center rounded-lg bg-red-100">
            <svg class="h-6 w-6 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
            </svg>
          </div>
          <div class="flex-1">
            <p class="text-sm font-medium text-red-800">Deactivate account</p>
            <p class="mt-1 text-xs text-red-700">
              Your account will be deactivated and your profile will be hidden.
              This action can potentially be reversed by contacting support.
            </p>
          </div>
        </div>

        @if (!showDeactivateForm()) {
          <div class="mt-4 border-t border-red-200 pt-4">
            <button
              (click)="showDeactivateForm.set(true)"
              class="rounded-lg border border-red-300 bg-white px-5 py-2.5 text-sm font-medium text-red-600 transition-colors hover:bg-red-600 hover:text-white hover:border-red-600"
            >
              Deactivate my account
            </button>
          </div>
        } @else {
          <form [formGroup]="form" (ngSubmit)="onSubmit()" class="mt-4 border-t border-red-200 pt-4 space-y-4">
            <app-form-field label="Password" fieldId="password" [control]="form.controls.password">
              <input
                id="password"
                formControlName="password"
                type="password"
                autocomplete="current-password"
                class="w-full rounded-lg border border-stone-200 bg-white px-3 py-2.5 text-sm text-stone-900 transition-colors placeholder:text-stone-400 focus:border-red-500 focus:outline-none"
                placeholder="Enter your password to confirm"
              />
            </app-form-field>

            <app-form-field label="Reason (optional)" fieldId="reason" [control]="form.controls.reason">
              <textarea
                id="reason"
                formControlName="reason"
                rows="3"
                class="w-full resize-none rounded-lg border border-stone-200 bg-white px-3 py-2.5 text-sm text-stone-900 transition-colors placeholder:text-stone-400 focus:border-red-500 focus:outline-none"
                placeholder="Tell us why you're leaving (optional)"
              ></textarea>
            </app-form-field>

            <div class="flex gap-3">
              <button
                type="submit"
                [disabled]="submitting()"
                class="inline-flex items-center gap-2 rounded-lg bg-red-600 px-5 py-3 text-sm font-medium text-white transition-colors hover:bg-red-700 disabled:opacity-50"
              >
                @if (submitting()) {
                  <div class="animate-spin h-4 w-4 rounded-full border-2 border-white/30 border-t-white"></div>
                  Deactivating…
                } @else {
                  Confirm deactivation
                }
              </button>
              <button
                type="button"
                (click)="showDeactivateForm.set(false)"
                class="rounded-lg border border-red-200 px-5 py-3 text-sm font-medium text-stone-600 transition-colors hover:border-red-300"
              >
                Cancel
              </button>
            </div>
          </form>
        }
      </div>
    </div>
  `,
})
export class DangerZone {
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly toast = inject(ToastService);

  protected readonly showDeactivateForm = signal(false);
  protected readonly submitting = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    password: ['', Validators.required],
    reason: [''],
  });

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    const { password, reason } = this.form.getRawValue();

    this.http
      .post<ApiResponse<void>>(`${environment.apiUrl}/api/profile/me/deactivate`, {
        password,
        reason: reason || null,
      })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          this.authService.logout();
          this.toast.success('Your account has been deactivated.');
        },
        error: (err) => {
          this.submitting.set(false);
          const apiErr = err.error as ApiError | undefined;
          this.toast.error(apiErr?.message || 'Failed to deactivate account.');
        },
      });
  }
}

