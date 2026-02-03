package com.reveria.userservice.controller;

import com.reveria.userservice.dto.SessionInfo;
import com.reveria.userservice.dto.request.auth.RefreshTokenRequest;
import com.reveria.userservice.dto.request.moderator.ModeratorChangePasswordRequest;
import com.reveria.userservice.dto.request.moderator.ModeratorLoginRequest;
import com.reveria.userservice.dto.response.ApiResponse;
import com.reveria.userservice.dto.response.ModeratorAuthResponse;
import com.reveria.userservice.dto.response.SessionResponse;
import com.reveria.userservice.security.JWTService;
import com.reveria.userservice.security.ModeratorPrincipal;
import com.reveria.userservice.service.ModeratorAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mod/auth")
@RequiredArgsConstructor
public class ModeratorAuthController {

    private final ModeratorAuthService moderatorAuthService;
    private final JWTService jwtService;

    // PUBLIC ENDPOINTS

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<ModeratorAuthResponse>> login(
            @Valid @RequestBody ModeratorLoginRequest request,
            HttpServletRequest httpRequest
    ) {
        SessionInfo sessionInfo = extractSessionInfo(httpRequest);
        ModeratorAuthResponse data = moderatorAuthService.login(
                request.getUsername(), request.getPassword(), sessionInfo
        );
        return ResponseEntity.ok(ApiResponse.success(data, "Login successful"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<ModeratorAuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest
    ) {
        SessionInfo sessionInfo = extractSessionInfo(httpRequest);
        ModeratorAuthResponse data = moderatorAuthService.refresh(request.getRefreshToken(), sessionInfo);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    // PROTECTED ENDPOINTS

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authHeader
    ) {
        String accessToken = authHeader.substring(7);
        String familyId = jwtService.extractFamilyId(accessToken);
        moderatorAuthService.logout(familyId);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Void>> logoutAll(
            @AuthenticationPrincipal ModeratorPrincipal principal
    ) {
        moderatorAuthService.logoutAll(principal.getModerator().getId());
        return ResponseEntity.ok(ApiResponse.success("All sessions revoked"));
    }

    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<List<SessionResponse>>> getActiveSessions(
            @RequestHeader("Authorization") String authHeader,
            @AuthenticationPrincipal ModeratorPrincipal principal
    ) {
        String accessToken = authHeader.substring(7);
        String currentFamilyId = jwtService.extractFamilyId(accessToken);
        List<SessionResponse> sessions = moderatorAuthService.getActiveSessions(
                principal.getModerator().getId(), currentFamilyId
        );
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    @DeleteMapping("/sessions/{familyId}")
    public ResponseEntity<ApiResponse<Void>> revokeSession(
            @AuthenticationPrincipal ModeratorPrincipal principal,
            @PathVariable String familyId
    ) {
        moderatorAuthService.revokeSession(principal.getModerator().getId(), familyId);
        return ResponseEntity.ok(ApiResponse.success("Session revoked"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ModeratorAuthResponse.ModeratorInfo>> getCurrentModerator(
            @AuthenticationPrincipal ModeratorPrincipal principal
    ) {
        ModeratorAuthResponse.ModeratorInfo info = moderatorAuthService.getModeratorInfo(
                principal.getModerator()
        );
        return ResponseEntity.ok(ApiResponse.success(info));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal ModeratorPrincipal principal,
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody ModeratorChangePasswordRequest request
    ) {
        String accessToken = authHeader.substring(7);
        String currentFamilyId = jwtService.extractFamilyId(accessToken);
        moderatorAuthService.changePassword(
                principal.getModerator().getId(),
                currentFamilyId,
                request.getCurrentPassword(),
                request.getNewPassword(),
                request.isRevokeOtherSessions()
        );
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
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
