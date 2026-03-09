import { Component, inject, OnInit, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../../core/models/api-response.model';
import { SessionResponse } from '../../../../core/models/session.model';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { DatePipe } from '@angular/common';

@Component({
  selector: 'app-sessions',
  imports: [DatePipe],
  template: `
    <div class="rounded-lg border border-stone-200 bg-white p-6 sm:p-8">
      <div class="mb-6 flex items-start justify-between">
        <div>
          <h2 class="text-lg font-bold text-stone-900">Active sessions</h2>
          <p class="mt-1 text-sm text-stone-500">Devices and browsers currently logged in to your account</p>
        </div>
        @if (sessions().length > 1) {
          <button
            (click)="logoutAll()"
            [disabled]="revokingAll()"
            class="shrink-0 rounded-lg border border-red-200 px-4 py-2.5 text-xs font-medium text-red-600 transition-colors hover:bg-red-50 hover:border-red-300 disabled:opacity-50"
          >
            Log out all
          </button>
        }
      </div>

      @if (loading()) {
        <div class="flex flex-col items-center py-16">
          <div class="animate-spin mb-4 h-8 w-8 rounded-full border border-stone-200 border-t-stone-600"></div>
          <p class="text-sm text-stone-500">Loading sessions…</p>
        </div>
      } @else if (sessions().length === 0) {
        <p class="py-8 text-center text-sm text-stone-500">No active sessions found.</p>
      } @else {
        <div class="space-y-3">
          @for (session of sessions(); track session.familyId) {
            <div
              class="flex items-center justify-between rounded-lg border p-4 transition-colors"
              [class]="session.currentSession
                ? 'border-stone-300 bg-stone-50'
                : 'border-stone-200 hover:border-stone-300'"
            >
              <div class="flex items-center gap-4">
                <div
                  class="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg"
                  [class]="session.currentSession ? 'bg-stone-200' : 'bg-stone-100'"
                >
                  <svg
                    class="h-5 w-5 text-stone-500"
                    fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"
                  >
                    @if (isMobile(session.userAgent)) {
                      <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 1.5H8.25A2.25 2.25 0 006 3.75v16.5a2.25 2.25 0 002.25 2.25h7.5A2.25 2.25 0 0018 20.25V3.75a2.25 2.25 0 00-2.25-2.25H13.5m-3 0V3h3V1.5m-3 0h3m-3 18.75h3" />
                    } @else {
                      <path stroke-linecap="round" stroke-linejoin="round" d="M9 17.25v1.007a3 3 0 01-.879 2.122L7.5 21h9l-.621-.621A3 3 0 0115 18.257V17.25m6-12V15a2.25 2.25 0 01-2.25 2.25H5.25A2.25 2.25 0 013 15V5.25A2.25 2.25 0 015.25 3h13.5A2.25 2.25 0 0121 5.25z" />
                    }
                  </svg>
                </div>
                <div class="min-w-0">
                  <div class="flex items-center gap-2">
                    <p class="truncate text-sm font-semibold text-stone-900">{{ parseBrowser(session.userAgent) }}</p>
                    @if (session.currentSession) {
                      <span class="shrink-0 rounded-full bg-stone-200 px-2 py-0.5 text-[10px] font-medium tracking-wide text-stone-600 uppercase">This device</span>
                    }
                  </div>
                  <p class="mt-0.5 text-xs text-stone-500">
                    {{ session.ipAddress }} · {{ session.createdAt | date: 'MMM d, y, h:mm a' }}
                  </p>
                </div>
              </div>

              @if (!session.currentSession) {
                <button
                  (click)="revokeSession(session.familyId)"
                  [disabled]="revoking() === session.familyId"
                  class="shrink-0 rounded-lg border border-stone-200 px-3 py-1.5 text-xs font-medium text-stone-600 transition-colors hover:border-red-200 hover:bg-red-50 hover:text-red-600 disabled:opacity-50"
                >
                  @if (revoking() === session.familyId) {
                    <div class="animate-spin h-3.5 w-3.5 rounded-full border-2 border-stone-300 border-t-stone-600"></div>
                  } @else {
                    Revoke
                  }
                </button>
              }
            </div>
          }
        </div>
      }
    </div>
  `,
})
export class Sessions implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);
  private readonly apiUrl = environment.apiUrl;

  protected readonly loading = signal(true);
  protected readonly sessions = signal<SessionResponse[]>([]);
  protected readonly revoking = signal<string | null>(null);
  protected readonly revokingAll = signal(false);

  ngOnInit(): void {
    this.loadSessions();
  }

  private loadSessions(): void {
    this.http.get<ApiResponse<SessionResponse[]>>(`${this.apiUrl}/api/auth/sessions`).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.data) this.sessions.set(res.data);
      },
      error: () => {
        this.loading.set(false);
        this.toast.error('Failed to load sessions.');
      },
    });
  }

  revokeSession(familyId: string): void {
    this.revoking.set(familyId);
    this.http.delete<ApiResponse<void>>(`${this.apiUrl}/api/auth/sessions/${familyId}`).subscribe({
      next: () => {
        this.revoking.set(null);
        this.sessions.update((s) => s.filter((session) => session.familyId !== familyId));
        this.toast.success('Session revoked.');
      },
      error: () => {
        this.revoking.set(null);
        this.toast.error('Failed to revoke session.');
      },
    });
  }

  logoutAll(): void {
    this.revokingAll.set(true);
    this.http.post<ApiResponse<void>>(`${this.apiUrl}/api/auth/logout-all`, {}).subscribe({
      next: () => {
        this.revokingAll.set(false);
        this.sessions.update((s) => s.filter((session) => session.currentSession));
        this.toast.success('All other sessions have been revoked.');
      },
      error: () => {
        this.revokingAll.set(false);
        this.toast.error('Failed to revoke sessions.');
      },
    });
  }

  parseBrowser(ua: string): string {
    if (!ua) return 'Unknown browser';
    if (ua.includes('Firefox')) return 'Firefox';
    if (ua.includes('Edg')) return 'Microsoft Edge';
    if (ua.includes('Chrome')) return 'Google Chrome';
    if (ua.includes('Safari')) return 'Safari';
    if (ua.includes('Opera') || ua.includes('OPR')) return 'Opera';
    return 'Unknown browser';
  }

  isMobile(ua: string): boolean {
    if (!ua) return false;
    return /Mobile|Android|iPhone|iPad/i.test(ua);
  }
}

