package com.reveria.userservice.service;

import com.reveria.userservice.dto.SessionInfo;
import com.reveria.userservice.dto.request.auth.LoginRequest;
import com.reveria.userservice.dto.request.auth.RegisterRequest;
import com.reveria.userservice.dto.response.AuthResponse;
import com.reveria.userservice.dto.response.SessionResponse;
import com.reveria.userservice.exception.InvalidCredentialsException;
import com.reveria.userservice.exception.PasswordMismatchException;
import com.reveria.userservice.model.entity.RefreshToken;
import com.reveria.userservice.model.entity.User;
import com.reveria.userservice.model.enums.AccountType;
import com.reveria.userservice.model.enums.UserStatus;
import com.reveria.userservice.exception.EmailAlreadyExistsException;
import com.reveria.userservice.exception.UsernameAlreadyExistsException;
import com.reveria.userservice.mapper.AuthMapper;
import com.reveria.userservice.mapper.SessionMapper;
import com.reveria.userservice.mapper.UserMapper;
import com.reveria.userservice.repository.UserRepository;
import com.reveria.userservice.security.JWTService;
import com.reveria.userservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final JWTService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;
    private final AuthMapper authMapper;
    private final SessionMapper sessionMapper;
    private final EmailVerificationService emailVerificationService;

    //REGISTER

    @Transactional
    public AuthResponse register(RegisterRequest request, SessionInfo sessionInfo) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyExistsException(request.getUsername());
        }

        User user = userMapper.toEntity(request);
        user.setEmail(request.getEmail().toLowerCase());
        user.setUsername(request.getUsername().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setDisplayName(request.getDisplayName() != null ? request.getDisplayName() : request.getUsername());
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(false);
        user.setLastLoginAt(LocalDateTime.now());

        user = userRepository.save(user);
        log.info("New user registered: {}", user.getUsername());
        emailVerificationService.sendVerificationEmail(user);

        return generateAuthResponse(user, sessionInfo, false);
    }

    //LOGIN

    @Transactional
    public AuthResponse login(LoginRequest request, SessionInfo sessionInfo) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getIdentifier(),
                        request.getPassword()
                )
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = principal.getUser();

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("User logged in: {}", user.getUsername());

        return generateAuthResponse(user, sessionInfo, request.isRememberMe());
    }

    //REFRESH TOKEN

    @Transactional
    public AuthResponse refresh(String refreshToken, SessionInfo sessionInfo) {
        RefreshToken newToken = refreshTokenService.rotateToken(refreshToken, sessionInfo);

        User user = newToken.getUser();
        UserPrincipal principal = new UserPrincipal(user);

        String accessToken = jwtService.generateAccessToken(principal, user.getUuid(), AccountType.USER,newToken.getFamilyId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationInSeconds())
                .build();
    }

    //LOGOUT

    @Transactional
    public void logout(String familyId) {
        refreshTokenService.revokeSession(familyId);
        log.info("Session logged out: {}", familyId);
    }

    @Transactional
    public void logoutAll(Long userId) {
        refreshTokenService.revokeAllUserSessions(userId);
        log.info("All sessions revoked for user: {}", userId);
    }

    //SESSION MANAGEMENT

    public List<SessionResponse> getActiveSessions(Long userId, String currentFamilyId) {
        return refreshTokenService.getActiveSessions(userId).stream()
                .map(token -> {
                    SessionResponse response = sessionMapper.toResponse(token);
                    response.setCurrentSession(token.getFamilyId().equals(currentFamilyId));
                    return response;
                })
                .toList();
    }

    @Transactional
    public void revokeSession(Long userId, String familyId) {
        List<RefreshToken> sessions = refreshTokenService.getActiveSessions(userId);
        boolean ownsSession = sessions.stream()
                .anyMatch(s -> s.getFamilyId().equals(familyId));

        if (!ownsSession) {
            throw new IllegalArgumentException("Session not found");
        }

        refreshTokenService.revokeSession(familyId);
        log.info("Session revoked: {} for user: {}", familyId, userId);
    }

    @Transactional
    public void changePassword(Long userId, String familyId, String currentPassword, String newPassword, boolean revokeOtherSessions) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean isOAuthOnlyUser = user.getPasswordHash() == null;

        if (isOAuthOnlyUser) {
            if (currentPassword != null && !currentPassword.isBlank()) {
                throw new IllegalArgumentException("Current password not required for OAuth users");
            }
        } else {
            if (currentPassword == null || currentPassword.isBlank()) {
                throw new IllegalArgumentException("Current password is required");
            }
            if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
                throw new PasswordMismatchException();
            }

            if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
                throw new IllegalArgumentException("New password must be different from current password");
            }
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        if (revokeOtherSessions) {
            revokeOtherSessions(userId, familyId);
        }
        log.info("Password changed for user: {}", user.getUsername());
    }
    private void revokeOtherSessions(Long userId, String currentFamilyId) {
        List<RefreshToken> sessions = refreshTokenService.getActiveSessions(userId);

        sessions.stream()
                .filter(session -> !session.getFamilyId().equals(currentFamilyId))
                .forEach(session -> refreshTokenService.revokeSession(session.getFamilyId()));
        log.info("Revoked {} other sessions for user: {}", sessions.size() - 1, userId);
    }

    private AuthResponse generateAuthResponse(User user, SessionInfo sessionInfo, boolean rememberMe) {
        UserPrincipal principal = new UserPrincipal(user);
        RefreshToken refreshToken = refreshTokenService.createSession(user, sessionInfo, rememberMe);
        String accessToken = jwtService.generateAccessToken(principal, user.getUuid(), AccountType.USER,refreshToken.getFamilyId());


        return authMapper.toAuthResponse(
                user,
                accessToken,
                refreshToken.getToken(),
                jwtService.getAccessTokenExpirationInSeconds()
        );
    }
}