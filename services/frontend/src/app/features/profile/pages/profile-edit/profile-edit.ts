import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { environment } from '../../../../environments/environment';
import { ApiResponse, ApiError } from '../../../../core/models/api-response.model';
import { UserProfileResponse, UpdateProfileRequest } from '../../../../core/models/profile.model';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { FormField } from '../../../../shared/components/form-field/form-field';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-profile-edit',
  imports: [ReactiveFormsModule, FormField, RouterLink],
  template: `
    <div class="animate-fade-up mx-auto max-w-3xl px-4 py-12">
      <!-- Header -->
      <div class="mb-8 flex items-center justify-between">
        <div>
          <h1 class="text-3xl font-bold text-stone-900">Edit profile</h1>
          <p class="mt-2 text-base text-stone-500">Update your personal information and avatar</p>
        </div>
        <a
          routerLink="/profile"
          class="inline-flex items-center gap-2 rounded-xl border-2 border-sand-200 px-4 py-2.5 text-sm font-semibold text-stone-600 transition-all duration-150 hover:border-sand-300 hover:text-stone-900"
        >
          <svg class="h-4.5 w-4.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 19.5L3 12m0 0l7.5-7.5M3 12h18" />
          </svg>
          Back
        </a>
      </div>

      @if (loading()) {
        <div class="flex flex-col items-center py-24">
          <div class="animate-spin-slow mb-5 h-10 w-10 rounded-full border-4 border-sand-200 border-t-coral-600"></div>
          <p class="text-base text-stone-500 font-medium">Loading...</p>
        </div>
      } @else {
        <!-- Avatar Card -->
        <div class="mb-6 rounded-3xl border-2 border-sand-200 bg-white p-8 shadow-2xl">
          <h2 class="mb-5 text-base font-bold text-stone-900">Avatar</h2>
          <div class="flex items-center gap-6">
            @if (avatarPreview() || profile()?.avatarUrl) {
              <img
                [src]="avatarPreview() || profile()!.avatarUrl!"
                alt="Avatar"
                class="h-24 w-24 rounded-2xl border-2 border-sand-100 object-cover shadow-lg"
              />
            } @else {
              <div
                class="flex h-24 w-24 items-center justify-center rounded-2xl bg-gradient-to-br from-coral-500 to-coral-600 text-3xl font-bold text-white shadow-lg"
              >
                {{ profile()?.displayName?.charAt(0)?.toUpperCase() || 'U' }}
              </div>
            }
            <div class="flex flex-col gap-3 sm:flex-row">
              <label
                class="btn-shine inline-flex cursor-pointer items-center gap-2.5 rounded-xl bg-gradient-to-r from-coral-500 to-coral-600 px-5 py-3 text-sm font-bold text-white shadow-lg shadow-coral-500/25 transition-all duration-200 hover:shadow-xl hover:-translate-y-0.5"
              >
                <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5m-13.5-9L12 3m0 0l4.5 4.5M12 3v13.5" />
                </svg>
                Upload
                <input type="file" accept="image/*" class="hidden" (change)="onAvatarSelected($event)" />
              </label>
              @if (profile()?.avatarUrl) {
                <button
                  (click)="deleteAvatar()"
                  class="inline-flex items-center gap-2.5 rounded-xl border-2 border-red-200 px-5 py-3 text-sm font-bold text-red-600 transition-all duration-150 hover:bg-red-50 hover:border-red-300"
                >
                  <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0" />
                  </svg>
                  Remove
                </button>
              }
            </div>
          </div>
        </div>

        <!-- Profile Form Card -->
        <div class="rounded-3xl border-2 border-sand-200 bg-white p-8 shadow-2xl">
          <h2 class="mb-6 text-base font-bold text-stone-900">Personal information</h2>
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
                class="w-full rounded-xl border-2 border-sand-200 bg-sand-50/50 px-4 py-3 text-sm text-stone-900 transition-all duration-200 placeholder:text-stone-400 focus:border-coral-500 focus:bg-white focus:ring-4 focus:ring-coral-500/10 focus:outline-none"
              />
            </app-form-field>

            <app-form-field label="Display Name" fieldId="displayName" [control]="form.controls.displayName">
              <input
                id="displayName"
                formControlName="displayName"
                type="text"
                class="w-full rounded-xl border-2 border-sand-200 bg-sand-50/50 px-4 py-3 text-sm text-stone-900 transition-all duration-200 placeholder:text-stone-400 focus:border-coral-500 focus:bg-white focus:ring-4 focus:ring-coral-500/10 focus:outline-none"
              />
            </app-form-field>

            <app-form-field label="Bio" fieldId="bio" [control]="form.controls.bio">
              <textarea
                id="bio"
                formControlName="bio"
                rows="4"
                class="w-full resize-none rounded-xl border-2 border-sand-200 bg-sand-50/50 px-4 py-3 text-sm text-stone-900 transition-all duration-200 placeholder:text-stone-400 focus:border-coral-500 focus:bg-white focus:ring-4 focus:ring-coral-500/10 focus:outline-none"
                placeholder="Tell us about yourself..."
              ></textarea>
            </app-form-field>

            <div class="flex gap-3 border-t-2 border-sand-100 pt-6">
              <button
                type="submit"
                [disabled]="submitting()"
                class="btn-shine inline-flex items-center justify-center gap-2.5 rounded-xl bg-gradient-to-r from-coral-500 to-coral-600 px-6 py-3.5 text-sm font-bold text-white shadow-lg shadow-coral-500/25 transition-all duration-200 hover:shadow-xl hover:shadow-coral-500/35 hover:-translate-y-0.5 disabled:opacity-50 disabled:shadow-none disabled:translate-y-0"
              >
                @if (submitting()) {
                  <div class="animate-spin-slow h-5 w-5 rounded-full border-2 border-white/30 border-t-white"></div>
                  Saving...
                } @else {
                  Save changes
                }
              </button>
              <button
                type="button"
                (click)="cancel()"
                class="rounded-xl border-2 border-sand-200 px-6 py-3.5 text-sm font-bold text-stone-600 transition-all duration-150 hover:border-sand-300 hover:text-stone-900"
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
