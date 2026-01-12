package com.reveria.userservice.controller;

import com.reveria.userservice.dto.SessionInfo;
import com.reveria.userservice.dto.request.LoginRequest;
import com.reveria.userservice.dto.request.RefreshTokenRequest;
import com.reveria.userservice.dto.request.RegisterRequest;
import com.reveria.userservice.dto.response.ApiResponse;
import com.reveria.userservice.dto.response.AuthResponse;
import com.reveria.userservice.dto.response.SessionResponse;
import com.reveria.userservice.security.JWTService;
import com.reveria.userservice.security.UserPrincipal;
import com.reveria.userservice.service.AuthService;
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

    private final AuthService authService;
    private final JWTService jwtService;

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
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Family-Id", required = false) String currentFamilyId
    ) {
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