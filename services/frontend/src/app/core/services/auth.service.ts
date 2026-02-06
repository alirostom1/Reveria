import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { catchError, firstValueFrom, of, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { TokenService } from './token.service';
import {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  ForgotPasswordRequest,
  ResetPasswordRequest,
  VerifyEmailRequest,
  UserInfo,
} from '../models/auth.model';
import { ApiResponse } from '../models/api-response.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly tokenService = inject(TokenService);
  private readonly apiUrl = environment.apiUrl;

  readonly currentUser = signal<UserInfo | null>(null);
  readonly isAuthenticated = computed(() => !!this.currentUser());

  async initAuth(): Promise<void> {
    if (!this.tokenService.hasTokens()) return;
    try {
      const res = await firstValueFrom(
        this.http.get<ApiResponse<UserInfo>>(`${this.apiUrl}/api/auth/me`).pipe(
          catchError(() => {
            this.tokenService.clearTokens();
            return of(null);
          })
        )
      );
      if (res?.data) {
        this.currentUser.set(res.data);
      }
    } catch {
      this.tokenService.clearTokens();
    }
  }

  login(request: LoginRequest) {
    return this.http
      .post<ApiResponse<AuthResponse>>(`${this.apiUrl}/api/auth/login`, request)
      .pipe(tap((res) => this.handleAuthResponse(res)));
  }

  register(request: RegisterRequest) {
    return this.http
      .post<ApiResponse<AuthResponse>>(`${this.apiUrl}/api/auth/register`, request)
      .pipe(tap((res) => this.handleAuthResponse(res)));
  }

  forgotPassword(request: ForgotPasswordRequest) {
    return this.http.post<ApiResponse<void>>(
      `${this.apiUrl}/api/auth/forgot-password`,
      request
    );
  }

  validateResetToken(token: string) {
    return this.http.get<ApiResponse<boolean>>(
      `${this.apiUrl}/api/auth/reset-password/validate`,
      { params: { token } }
    );
  }

  resetPassword(request: ResetPasswordRequest) {
    return this.http.post<ApiResponse<void>>(
      `${this.apiUrl}/api/auth/reset-password`,
      request
    );
  }

  verifyEmail(request: VerifyEmailRequest) {
    return this.http.post<ApiResponse<void>>(
      `${this.apiUrl}/api/auth/verify-email`,
      request
    );
  }

  refreshToken() {
    const refreshToken = this.tokenService.getRefreshToken();
    return this.http
      .post<ApiResponse<AuthResponse>>(`${this.apiUrl}/api/auth/refresh`, { refreshToken })
      .pipe(tap((res) => this.handleAuthResponse(res)));
  }

  logout(): void {
    const token = this.tokenService.getAccessToken();
    if (token) {
      this.http
        .post(`${this.apiUrl}/api/auth/logout`, {})
        .pipe(catchError(() => of(null)))
        .subscribe();
    }
    this.tokenService.clearTokens();
    this.currentUser.set(null);
    this.router.navigateByUrl('/');
  }

  private handleAuthResponse(res: ApiResponse<AuthResponse>): void {
    if (res.data) {
      this.tokenService.setTokens(res.data.accessToken, res.data.refreshToken);
      this.currentUser.set(res.data.user);
    }
  }
}
