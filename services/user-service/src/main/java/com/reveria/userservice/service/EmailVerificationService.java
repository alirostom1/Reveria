package com.reveria.userservice.service;

import com.reveria.userservice.model.entity.EmailVerificationToken;
import com.reveria.userservice.model.entity.User;
import com.reveria.userservice.exception.InvalidTokenException;
import com.reveria.userservice.model.enums.UserStatus;
import com.reveria.userservice.repository.EmailVerificationTokenRepository;
import com.reveria.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${security.email-verification-expiration-hours:24}")
    private int tokenExpirationHours;

    private static final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public void sendVerificationEmail(User user) {
        if (user.getEmailVerified()) {
            log.info("Email already verified for user: {}", user.getUsername());
            return;
        }
        tokenRepository.invalidateAllByUserId(user.getId());

        String token = generateSecureToken();

        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusHours(tokenExpirationHours))
                .used(false)
                .build();

        tokenRepository.save(verificationToken);

        emailService.sendEmailVerification(
                user.getEmail(),
                user.getDisplayName(),
                token,
                tokenExpirationHours
        );

        log.info("Verification email sent to user: {}", user.getUsername());
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email.toLowerCase());

        if (userOptional.isEmpty()) {
            log.info("Resend verification requested for non-existent email: {}", email);
            return;
        }

        User user = userOptional.get();

        if (user.getEmailVerified()) {
            log.info("Resend verification requested for already verified email: {}", email);
            return;
        }
        //RATE LIMITING (2 MINS BEFORE A NEW REQUEST)
        Optional<EmailVerificationToken> existingToken = tokenRepository.findByUserIdAndUsedFalse(user.getId());
        if (existingToken.isPresent()) {
            LocalDateTime tokenCreatedAt = existingToken.get().getCreatedAt();
            if (tokenCreatedAt.plusMinutes(2).isAfter(LocalDateTime.now())) {
                log.info("Rate limit: verification email recently sent for user: {}", user.getUsername());
                return;
            }
        }
        sendVerificationEmail(user);
    }


    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid verification token"));

        if (verificationToken.getUsed()) {
            throw new InvalidTokenException("Verification token has already been used");
        }

        if (verificationToken.isExpired()) {
            throw new InvalidTokenException("Verification token has expired");
        }

        User user = verificationToken.getUser();

        if (user.getEmailVerified()) {
            throw new InvalidTokenException("Email is already verified");
        }

        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        verificationToken.setUsed(true);
        tokenRepository.save(verificationToken);

        emailService.sendWelcomeEmail(user.getEmail(), user.getDisplayName());

        log.info("Email verified for user: {}", user.getUsername());
    }


    public boolean validateToken(String token) {
        return tokenRepository.findByToken(token)
                .map(EmailVerificationToken::isValid)
                .orElse(false);
    }


    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }


    @Scheduled(cron = "0 0 */6 * * *") // Every 6 hours
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Cleaning up expired email verification tokens");
        tokenRepository.deleteExpiredTokens(LocalDateTime.now());
        tokenRepository.deleteUsedTokensOlderThan(LocalDateTime.now().minusDays(7));
    }
}