package com.reveria.userservice.controller;

import com.reveria.userservice.dto.SessionInfo;
import com.reveria.userservice.dto.request.auth.*;
import com.reveria.userservice.dto.response.ApiResponse;
import com.reveria.userservice.dto.response.AuthResponse;
import com.reveria.userservice.dto.response.SessionResponse;
import com.reveria.userservice.model.enums.ProviderType;
import com.reveria.userservice.security.JWTService;
import com.reveria.userservice.security.UserPrincipal;
import com.reveria.userservice.service.AuthService;
import com.reveria.userservice.service.EmailVerificationService;
import com.reveria.userservice.service.OAuthService;
import com.reveria.userservice.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final PasswordResetService passwordResetService;


    private final AuthService authService;
    private final JWTService jwtService;
    private final OAuthService oAuthService;
    private final EmailVerificationService emailVerificationService;

    //  PUBLIC ENDPOINTS

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest
    ) {
        SessionInfo sessionInfo = extractSessionInfo(httpRequest);
        AuthResponse data = authService.register(request, sessionInfo);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(data, "Registration successful"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        SessionInfo sessionInfo = extractSessionInfo(httpRequest);
        AuthResponse data = authService.login(request, sessionInfo);
        return ResponseEntity.ok(ApiResponse.success(data, "Login successful"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest
    ) {
        SessionInfo sessionInfo = extractSessionInfo(httpRequest);
        AuthResponse data = authService.refresh(request.getRefreshToken(), sessionInfo);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    //  PROTECTED ENDPOINTS

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authHeader
    ) {
        String accessToken = authHeader.substring(7);
        String familyId = jwtService.extractFamilyId(accessToken);
        authService.logout(familyId);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Void>> logoutAll(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        authService.logoutAll(principal.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("All sessions revoked"));
    }

    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<List<SessionResponse>>> getActiveSessions(
            @RequestHeader("Authorization") String authHeader,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        String accessToken = authHeader.substring(7);
        String currentFamilyId = jwtService.extractFamilyId(accessToken);
        List<SessionResponse> sessions = authService.getActiveSessions(
                principal.getUser().getId(),
                currentFamilyId
        );
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    @DeleteMapping("/sessions/{familyId}")
    public ResponseEntity<ApiResponse<Void>> revokeSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String familyId
    ) {
        authService.revokeSession(principal.getUser().getId(), familyId);
        return ResponseEntity.ok(ApiResponse.success("Session revoked"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse.UserInfo>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
                .uuid(principal.getUser().getUuid())
                .email(principal.getUser().getEmail())
                .username(principal.getUser().getUsername())
                .displayName(principal.getUser().getDisplayName())
                .avatarUrl(principal.getUser().getAvatarUrl())
                .build();

        return ResponseEntity.ok(ApiResponse.success(userInfo));
    }
    // OAUTH ENDPOINTS
    @PostMapping("/oauth")
    public ResponseEntity<ApiResponse<AuthResponse>> oauthLogin(
            @Valid @RequestBody OAuthLoginRequest request,
            HttpServletRequest httpRequest
    ) {
        SessionInfo sessionInfo = extractSessionInfo(httpRequest);
        AuthResponse response = oAuthService.authenticate(
                request.getProvider(),
                request.getCode(),
                request.getRedirectUri(),
                sessionInfo
        );
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @PostMapping("/oauth/link")
    public ResponseEntity<ApiResponse<Void>> linkProvider(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody OAuthLoginRequest request
    ) {
        oAuthService.linkProvider(
                principal.getUser().getId(),
                request.getProvider(),
                request.getCode(),
                request.getRedirectUri()
        );
        return ResponseEntity.ok(ApiResponse.success("Provider linked successfully"));
    }

    @DeleteMapping("/oauth/{provider}")
    public ResponseEntity<ApiResponse<Void>> unlinkProvider(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable ProviderType provider
    ) {
        oAuthService.unlinkProvider(principal.getUser().getId(), provider);
        return ResponseEntity.ok(ApiResponse.success("Provider unlinked successfully"));
    }

    // PASSWORD ENDPOINTS

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        passwordResetService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("If the email exists, a reset link has been sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Password reset successful"));
    }

    @GetMapping("/reset-password/validate")
    public ResponseEntity<ApiResponse<Boolean>> validateResetToken(
            @RequestParam String token
    ) {
        boolean valid = passwordResetService.validateToken(token);
        return ResponseEntity.ok(ApiResponse.success(valid));
    }
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody ChangePasswordRequest request
    ){
        String accessToken = authHeader.substring(7);
        String currentFamilyId = jwtService.extractFamilyId(accessToken);
        authService.changePassword(
                principal.getUser().getId(),
                currentFamilyId,
                request.getCurrentPassword(),
                request.getNewPassword(),
                request.isRevokeOtherSessions()
        );
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    // EMAIL VERIFICATION
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request
    ) {
        emailVerificationService.verifyEmail(request.getToken());
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully"));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request
    ) {
        emailVerificationService.resendVerificationEmail(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("If the email exists and is unverified, a verification link has been sent"));
    }

    @GetMapping("/verify-email/validate")
    public ResponseEntity<ApiResponse<Boolean>> validateVerificationToken(
            @RequestParam String token
    ) {
        boolean valid = emailVerificationService.validateToken(token);
        return ResponseEntity.ok(ApiResponse.success(valid));
    }

    // HELPER

    private SessionInfo extractSessionInfo(HttpServletRequest request) {
        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        return new SessionInfo(ipAddress, userAgent);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}