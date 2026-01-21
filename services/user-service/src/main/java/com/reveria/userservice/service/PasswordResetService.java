package com.reveria.userservice.service;

import com.reveria.userservice.model.entity.PasswordResetToken;
import com.reveria.userservice.model.entity.User;
import com.reveria.userservice.exception.InvalidTokenException;
import com.reveria.userservice.repository.PasswordResetTokenRepository;
import com.reveria.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${security.password-reset-expiration-hours:1}")
    private int tokenExpirationHours;

    private static final SecureRandom secureRandom = new SecureRandom();


    @Transactional
    public void requestPasswordReset(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email.toLowerCase());

        if (userOptional.isEmpty()) {
            log.info("Password reset requested for non-existent email: {}", email);
            return;
        }

        User user = userOptional.get();

        if (user.getPasswordHash() == null && !user.getAuthProviders().isEmpty()) {
            log.info("Password reset requested for OAuth-only user: {}", email);
            return;
        }

        tokenRepository.invalidateAllByUserId(user.getId());

        String token = generateSecureToken();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusHours(tokenExpirationHours))
                .used(false)
                .build();

        tokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(
                user.getEmail(),
                user.getDisplayName(),
                token,
                tokenExpirationHours
        );

        log.info("Password reset token generated for user: {}", user.getUsername());
    }


    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired reset token"));

        if (resetToken.getUsed()) {
            throw new InvalidTokenException("Reset token has already been used");
        }

        if (resetToken.isExpired()) {
            throw new InvalidTokenException("Reset token has expired");
        }

        User user = resetToken.getUser();

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        refreshTokenService.revokeAllUserSessions(user.getId());

        log.info("Password reset successful for user: {}", user.getUsername());
    }


    public boolean validateToken(String token) {
        return tokenRepository.findByToken(token)
                .map(t -> !t.getUsed() && !t.isExpired())
                .orElse(false);
    }


    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }


    @Scheduled(cron = "0 0 */6 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Cleaning up expired password reset tokens");
        tokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }
}