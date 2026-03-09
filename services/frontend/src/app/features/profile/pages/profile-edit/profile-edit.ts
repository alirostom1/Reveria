import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { environment } from '../../../../environments/environment';
import { ApiResponse, ApiError } from '../../../../core/models/api-response.model';
import { UserProfileResponse, UpdateProfileRequest } from '../../../../core/models/profile.model';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { FormField } from '../../../../shared/components/form-field/form-field';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-profile-edit',
  imports: [ReactiveFormsModule, FormField],
  template: `
    <div class="mx-auto max-w-3xl px-4 py-8 sm:px-6">
      <!-- Header -->
      <div class="mb-6">
        <h1 class="text-2xl font-bold text-stone-900">Edit profile</h1>
        <p class="mt-1 text-sm text-stone-500">Update your personal information and avatar</p>
      </div>

      @if (loading()) {
        <div class="flex items-center justify-center py-24">
          <div class="h-8 w-8 animate-spin rounded-full border-2 border-stone-200 border-t-stone-600"></div>
        </div>
      } @else {
        <!-- Avatar Card -->
        <div class="mb-6 rounded-lg border border-stone-200 bg-white p-6">
          <h2 class="mb-4 text-sm font-medium text-stone-900">Avatar</h2>
          <div class="flex items-center gap-5">
            @if (avatarPreview() || profile()?.avatarUrl) {
              <img
                [src]="avatarPreview() || profile()!.avatarUrl!"
                alt="Avatar"
                class="h-20 w-20 rounded-full border border-stone-200 object-cover"
              />
            } @else {
              <div
                class="flex h-20 w-20 items-center justify-center rounded-full bg-coral-600 text-2xl font-bold text-white"
              >
                {{ profile()?.displayName?.charAt(0)?.toUpperCase() || 'U' }}
              </div>
            }
            <div class="flex flex-col gap-2 sm:flex-row">
              <label
                class="inline-flex cursor-pointer items-center gap-2 rounded-lg bg-coral-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-coral-700"
              >
                Upload
                <input type="file" accept="image/*" class="hidden" (change)="onAvatarSelected($event)" />
              </label>
              @if (profile()?.avatarUrl) {
                <button
                  (click)="deleteAvatar()"
                  class="inline-flex items-center gap-2 rounded-lg border border-stone-200 px-4 py-2 text-sm font-medium text-stone-600 transition-colors hover:border-red-200 hover:bg-red-50 hover:text-red-600"
                >
                  Remove
                </button>
              }
            </div>
          </div>
        </div>

        <!-- Profile Form Card -->
        <div class="rounded-lg border border-stone-200 bg-white p-6">
          <h2 class="mb-5 text-sm font-medium text-stone-900">Personal information</h2>
          <form [formGroup]="form" (ngSubmit)="onSubmit()">
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
                class="w-full rounded-lg border border-stone-200 bg-white px-3 py-2.5 text-sm text-stone-900 placeholder:text-stone-400 focus:border-stone-300 focus:outline-none"
              />
            </app-form-field>

            <app-form-field label="Display Name" fieldId="displayName" [control]="form.controls.displayName">
              <input
                id="displayName"
                formControlName="displayName"
                type="text"
                class="w-full rounded-lg border border-stone-200 bg-white px-3 py-2.5 text-sm text-stone-900 placeholder:text-stone-400 focus:border-stone-300 focus:outline-none"
              />
            </app-form-field>

            <app-form-field label="Bio" fieldId="bio" [control]="form.controls.bio">
              <textarea
                id="bio"
                formControlName="bio"
                rows="4"
                class="w-full resize-none rounded-lg border border-stone-200 bg-white px-3 py-2.5 text-sm text-stone-900 placeholder:text-stone-400 focus:border-stone-300 focus:outline-none"
                placeholder="Tell us about yourself..."
              ></textarea>
            </app-form-field>

            <div class="flex gap-2.5 border-t border-stone-100 pt-5">
              <button
                type="submit"
                [disabled]="submitting()"
                class="inline-flex items-center justify-center gap-2 rounded-lg bg-coral-600 px-5 py-2.5 text-sm font-medium text-white transition-colors hover:bg-coral-700 disabled:opacity-50"
              >
                @if (submitting()) {
                  <div class="h-4 w-4 animate-spin rounded-full border-2 border-white/30 border-t-white"></div>
                  Saving...
                } @else {
                  Save changes
                }
              </button>
              <button
                type="button"
                (click)="cancel()"
                class="rounded-lg border border-stone-200 px-5 py-2.5 text-sm font-medium text-stone-600 transition-colors hover:border-stone-300 hover:text-stone-900"
              >
                Cancel
              </button>
            </div>
          </form>
        </div>
      }
    </div>
  `,
})
export class ProfileEdit implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly authService = inject(AuthService);
  private readonly apiUrl = environment.apiUrl;

  protected readonly loading = signal(true);
  protected readonly submitting = signal(false);
  protected readonly profile = signal<UserProfileResponse | null>(null);
  protected readonly avatarPreview = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(30), Validators.pattern(/^\w+$/)]],
    displayName: ['', Validators.maxLength(50)],
    bio: ['', Validators.maxLength(500)],
  });

  ngOnInit(): void {
    this.http.get<ApiResponse<UserProfileResponse>>(`${this.apiUrl}/api/profile/me`).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.data) {
          this.profile.set(res.data);
          this.form.patchValue({
            username: res.data.username,
            displayName: res.data.displayName,
            bio: res.data.bio || '',
          });
        }
      },
      error: () => this.loading.set(false),
    });
  }

  onAvatarSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = () => this.avatarPreview.set(reader.result as string);
    reader.readAsDataURL(file);

    const formData = new FormData();
    formData.append('file', file);

    this.http
      .post<ApiResponse<UserProfileResponse>>(`${this.apiUrl}/api/profile/me/avatar`, formData)
      .subscribe({
        next: (res) => {
          if (res.data) {
            this.profile.set(res.data);
            this.updateCurrentUser(res.data);
            this.toast.success('Avatar updated!');
          }
        },
        error: () => {
          this.avatarPreview.set(null);
          this.toast.error('Failed to upload avatar.');
        },
      });
  }

  deleteAvatar(): void {
    this.http
      .delete<ApiResponse<UserProfileResponse>>(`${this.apiUrl}/api/profile/me/avatar`)
      .subscribe({
        next: (res) => {
          if (res.data) {
            this.profile.set(res.data);
            this.avatarPreview.set(null);
            this.updateCurrentUser(res.data);
            this.toast.success('Avatar removed.');
          }
        },
        error: () => this.toast.error('Failed to remove avatar.'),
      });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    const body: UpdateProfileRequest = this.form.getRawValue();

    this.http.patch<ApiResponse<UserProfileResponse>>(`${this.apiUrl}/api/profile/me`, body).subscribe({
      next: (res) => {
        this.submitting.set(false);
        if (res.data) {
          this.profile.set(res.data);
          this.updateCurrentUser(res.data);
          this.toast.success('Profile updated!');
          this.router.navigateByUrl('/profile');
        }
      },
      error: (err) => {
        this.submitting.set(false);
        const apiErr = err.error as ApiError | undefined;
        this.toast.error(apiErr?.message || 'Failed to update profile.');
      },
    });
  }

  cancel(): void {
    this.router.navigateByUrl('/profile');
  }

  private updateCurrentUser(profile: UserProfileResponse): void {
    this.authService.currentUser.set({
      uuid: profile.uuid,
      email: profile.email,
      username: profile.username,
      displayName: profile.displayName,
      avatarUrl: profile.avatarUrl,
    });
  }
}
