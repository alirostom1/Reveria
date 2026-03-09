import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { ApiResponse, ApiError } from '../../../../core/models/api-response.model';
import { PrivacySettings, ProfileVisibility, MessagePrivacy } from '../../../../core/models/privacy.model';
import { ToastService } from '../../../../shared/components/toast/toast.service';

@Component({
  selector: 'app-privacy-settings',
  imports: [ReactiveFormsModule],
  template: `
    <div class="rounded-lg border border-stone-200 bg-white p-6 sm:p-8">
      <div class="mb-6">
        <h2 class="text-lg font-bold text-stone-900">Privacy settings</h2>
        <p class="mt-1 text-sm text-stone-500">Control who can see your profile and interact with you</p>
      </div>

      @if (loading()) {
        <div class="flex flex-col items-center py-16">
          <div class="animate-spin mb-4 h-8 w-8 rounded-full border border-stone-200 border-t-stone-600"></div>
          <p class="text-sm text-stone-500">Loading…</p>
        </div>
      } @else {
        <form [formGroup]="form" (ngSubmit)="onSubmit()" class="space-y-6">
          <!-- Profile Visibility -->
          <div>
            <label class="mb-2 block text-sm font-semibold text-stone-800">Profile visibility</label>
            <select
              formControlName="profileVisibility"
              class="w-full rounded-lg border border-stone-200 bg-stone-50/50 px-4 py-3 text-sm text-stone-900 transition-colors focus:border-coral-500 focus:bg-white focus:ring-4 focus:ring-coral-500/10 focus:outline-none"
            >
              <option value="PUBLIC">Public — Anyone can view</option>
              <option value="FRIENDS_ONLY">Friends only</option>
              <option value="PRIVATE">Private — Only you</option>
            </select>
          </div>

          <!-- Message Privacy -->
          <div>
            <label class="mb-2 block text-sm font-semibold text-stone-800">Who can message you</label>
            <select
              formControlName="messagePrivacy"
              class="w-full rounded-lg border border-stone-200 bg-stone-50/50 px-4 py-3 text-sm text-stone-900 transition-colors focus:border-coral-500 focus:bg-white focus:ring-4 focus:ring-coral-500/10 focus:outline-none"
            >
              <option value="EVERYONE">Everyone</option>
              <option value="FRIENDS_ONLY">Friends only</option>
              <option value="NOBODY">Nobody</option>
            </select>
          </div>

          <!-- Toggles -->
          <div class="space-y-3">
            <label class="flex cursor-pointer items-center justify-between rounded-lg border border-stone-200 bg-stone-50/30 p-4 transition-colors hover:bg-stone-100/50">
              <div>
                <p class="text-sm font-semibold text-stone-800">Show online status</p>
                <p class="text-xs text-stone-500">Let others see when you're online</p>
              </div>
              <input
                type="checkbox"
                formControlName="showOnlineStatus"
                class="h-5 w-5 rounded border-stone-300 text-coral-600 focus:ring-coral-600"
              />
            </label>

            <label class="flex cursor-pointer items-center justify-between rounded-lg border border-stone-200 bg-stone-50/30 p-4 transition-colors hover:bg-stone-100/50">
              <div>
                <p class="text-sm font-semibold text-stone-800">Allow direct messages</p>
                <p class="text-xs text-stone-500">Receive direct messages from other users</p>
              </div>
              <input
                type="checkbox"
                formControlName="allowDirectMessages"
                class="h-5 w-5 rounded border-stone-300 text-coral-600 focus:ring-coral-600"
              />
            </label>

            <label class="flex cursor-pointer items-center justify-between rounded-lg border border-stone-200 bg-stone-50/30 p-4 transition-colors hover:bg-stone-100/50">
              <div>
                <p class="text-sm font-semibold text-stone-800">Allow friend requests</p>
                <p class="text-xs text-stone-500">Let others send you friend requests</p>
              </div>
              <input
                type="checkbox"
                formControlName="allowFriendRequests"
                class="h-5 w-5 rounded border-stone-300 text-coral-600 focus:ring-coral-600"
              />
            </label>
          </div>

          <button
            type="submit"
            [disabled]="submitting()"
            class="inline-flex items-center justify-center gap-2 rounded-lg bg-coral-600 px-5 py-2.5 text-sm font-medium text-white transition-colors hover:bg-coral-700 disabled:opacity-50"
          >
            @if (submitting()) {
              <div class="animate-spin h-5 w-5 rounded-full border-2 border-white/30 border-t-white"></div>
              Saving…
            } @else {
              Save settings
            }
          </button>
        </form>
      }
    </div>
  `,
})
export class PrivacySettingsPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);
  private readonly apiUrl = environment.apiUrl;

  protected readonly loading = signal(true);
  protected readonly submitting = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    profileVisibility: [ProfileVisibility.PUBLIC],
    showOnlineStatus: [true],
    allowDirectMessages: [true],
    allowFriendRequests: [true],
    messagePrivacy: [MessagePrivacy.EVERYONE],
  });

  ngOnInit(): void {
    this.http.get<ApiResponse<PrivacySettings>>(`${this.apiUrl}/api/profile/me/privacy`).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.data) {
          this.form.patchValue(res.data);
        }
      },
      error: () => this.loading.set(false),
    });
  }

  onSubmit(): void {
    this.submitting.set(true);
    this.http
      .patch<ApiResponse<PrivacySettings>>(`${this.apiUrl}/api/profile/me/privacy`, this.form.getRawValue())
      .subscribe({
        next: () => {
          this.submitting.set(false);
          this.toast.success('Privacy settings updated!');
        },
        error: (err) => {
          this.submitting.set(false);
          const apiErr = err.error as ApiError | undefined;
          this.toast.error(apiErr?.message || 'Failed to update privacy settings.');
        },
      });
  }
}

