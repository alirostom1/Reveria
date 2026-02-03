package com.reveria.userservice.service;

import com.reveria.userservice.dto.SessionInfo;
import com.reveria.userservice.dto.response.ModeratorAuthResponse;
import com.reveria.userservice.dto.response.SessionResponse;
import com.reveria.userservice.exception.InvalidCredentialsException;
import com.reveria.userservice.exception.ModeratorAccountDeactivatedException;
import com.reveria.userservice.exception.PasswordMismatchException;
import com.reveria.userservice.mapper.ModeratorAuthMapper;
import com.reveria.userservice.mapper.SessionMapper;
import com.reveria.userservice.model.entity.Moderator;
import com.reveria.userservice.model.entity.RefreshToken;
import com.reveria.userservice.model.enums.AccountType;
import com.reveria.userservice.repository.ModeratorRepository;
import com.reveria.userservice.security.JWTService;
import com.reveria.userservice.security.ModeratorPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModeratorAuthService {

    private final ModeratorRepository moderatorRepository;
    private final RefreshTokenService refreshTokenService;
    private final JWTService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final ModeratorAuthMapper moderatorAuthMapper;
    private final SessionMapper sessionMapper;
    private final LoginAttemptService loginAttemptService;

    // LOGIN

    @Transactional
    public ModeratorAuthResponse login(String username, String password, SessionInfo sessionInfo) {
        loginAttemptService.checkLoginAllowed(username, sessionInfo.ipAddress());

        Moderator moderator = moderatorRepository.findByUsername(username.toLowerCase())
                .orElse(null);

        if (moderator == null) {
            loginAttemptService.recordFailedLogin(
                    username, sessionInfo.ipAddress(), sessionInfo.userAgent(),
                    AccountType.MODERATOR, "User not found"
            );
            throw new InvalidCredentialsException();
        }

        if (!moderator.getActive()) {
            loginAttemptService.recordFailedLogin(
                    username, sessionInfo.ipAddress(), sessionInfo.userAgent(),
                    AccountType.MODERATOR, "Account deactivated"
            );
            throw new ModeratorAccountDeactivatedException();
        }

        if (!passwordEncoder.matches(password, moderator.getPasswordHash())) {
            loginAttemptService.recordFailedLogin(
                    username, sessionInfo.ipAddress(), sessionInfo.userAgent(),
                    AccountType.MODERATOR, "Invalid credentials"
            );
            throw new InvalidCredentialsException();
        }

        loginAttemptService.recordSuccessfulLogin(
                username, sessionInfo.ipAddress(), sessionInfo.userAgent(), AccountType.MODERATOR
        );

        moderator.setLastLoginAt(LocalDateTime.now());
        moderatorRepository.save(moderator);

        log.info("Moderator logged in: {}", moderator.getUsername());
        return generateAuthResponse(moderator, sessionInfo);
    }

    // REFRESH

    @Transactional
    public ModeratorAuthResponse refresh(String refreshToken, SessionInfo sessionInfo) {
        RefreshToken newToken = refreshTokenService.rotateToken(refreshToken, sessionInfo);

        Moderator moderator = newToken.getModerator();
        ModeratorPrincipal principal = new ModeratorPrincipal(moderator);

        String accessToken = jwtService.generateAccessToken(
                principal, moderator.getUuid(), AccountType.MODERATOR, newToken.getFamilyId()
        );

        return ModeratorAuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationInSeconds())
                .build();
    }

    // LOGOUT

    @Transactional
    public void logout(String familyId) {
        refreshTokenService.revokeSession(familyId);
        log.info("Moderator session logged out: {}", familyId);
    }

    @Transactional
    public void logoutAll(Long moderatorId) {
        refreshTokenService.revokeAllModeratorSessions(moderatorId);
        log.info("All sessions revoked for moderator: {}", moderatorId);
    }

    // SESSION MANAGEMENT

    public List<SessionResponse> getActiveSessions(Long moderatorId, String currentFamilyId) {
        return refreshTokenService.getActiveModeratorSessions(moderatorId).stream()
                .map(token -> {
                    SessionResponse response = sessionMapper.toResponse(token);
                    response.setCurrentSession(token.getFamilyId().equals(currentFamilyId));
                    return response;
                })
                .toList();
    }

    @Transactional
    public void revokeSession(Long moderatorId, String familyId) {
        List<RefreshToken> sessions = refreshTokenService.getActiveModeratorSessions(moderatorId);
        boolean ownsSession = sessions.stream()
                .anyMatch(s -> s.getFamilyId().equals(familyId));

        if (!ownsSession) {
            throw new IllegalArgumentException("Session not found");
        }

        refreshTokenService.revokeSession(familyId);
        log.info("Moderator session revoked: {} for moderator: {}", familyId, moderatorId);
    }

    // CHANGE PASSWORD

    @Transactional
    public void changePassword(Long moderatorId, String familyId,
                                String currentPassword, String newPassword,
                                boolean revokeOtherSessions) {
        Moderator moderator = moderatorRepository.findById(moderatorId)
                .orElseThrow(() -> new IllegalArgumentException("Moderator not found"));

        if (!passwordEncoder.matches(currentPassword, moderator.getPasswordHash())) {
            throw new PasswordMismatchException();
        }

        if (passwordEncoder.matches(newPassword, moderator.getPasswordHash())) {
            throw new IllegalArgumentException("New password must be different from current password");
        }

        moderator.setPasswordHash(passwordEncoder.encode(newPassword));
        moderatorRepository.save(moderator);

        if (revokeOtherSessions) {
            revokeOtherSessions(moderatorId, familyId);
        }

        log.info("Password changed for moderator: {}", moderator.getUsername());
    }

    // GET CURRENT MODERATOR INFO

    public ModeratorAuthResponse.ModeratorInfo getModeratorInfo(Moderator moderator) {
        return moderatorAuthMapper.toModeratorInfo(moderator);
    }

    // PRIVATE

    private void revokeOtherSessions(Long moderatorId, String currentFamilyId) {
        List<RefreshToken> sessions = refreshTokenService.getActiveModeratorSessions(moderatorId);

        sessions.stream()
                .filter(session -> !session.getFamilyId().equals(currentFamilyId))
                .forEach(session -> refreshTokenService.revokeSession(session.getFamilyId()));

        log.info("Revoked {} other sessions for moderator: {}", sessions.size() - 1, moderatorId);
    }

    private ModeratorAuthResponse generateAuthResponse(Moderator moderator, SessionInfo sessionInfo) {
        ModeratorPrincipal principal = new ModeratorPrincipal(moderator);
        RefreshToken refreshToken = refreshTokenService.createSession(moderator, sessionInfo);
        String accessToken = jwtService.generateAccessToken(
                principal, moderator.getUuid(), AccountType.MODERATOR, refreshToken.getFamilyId()
        );

        return moderatorAuthMapper.toAuthResponse(
                moderator,
                accessToken,
                refreshToken.getToken(),
                jwtService.getAccessTokenExpirationInSeconds()
        );
    }
}
