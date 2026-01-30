package com.reveria.userservice.service;

import com.reveria.userservice.config.RateLimitConfig;
import com.reveria.userservice.exception.AccountLockedException;
import com.reveria.userservice.exception.TooManyRequestsException;
import com.reveria.userservice.model.entity.LoginAttempt;
import com.reveria.userservice.model.enums.AccountType;
import com.reveria.userservice.repository.LoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Brute Force Protection Service
 *
 * Protects against brute force attacks using two strategies:
 *
 * 1. ACCOUNT LOCKOUT: After 5 failed attempts on the same account within 1 hour,
 *    that account is locked for 15 minutes. This prevents attackers from
 *    guessing passwords for a specific user.
 *
 * 2. IP BLOCKING: After 20 failed attempts from the same IP within 1 hour
 *    (regardless of which accounts), that IP is blocked for 30 minutes.
 *    This prevents attackers from trying different usernames from one location.
 *
 * How it works:
 * - Every login attempt (success or failure) is recorded in the database
 * - Before each login, we check if the account or IP should be blocked
 * - On successful login, the attempt is recorded (for audit purposes)
 * - On failed login, the attempt is recorded with the failure reason
 * - Old records are cleaned up daily to prevent database bloat
 *
 * NB: You can adjust nb of attempts and time window as you like in application.properties
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginAttemptService {

    private final LoginAttemptRepository loginAttemptRepository;
    private final RateLimitConfig config;


    public void checkLoginAllowed(String identifier, String ipAddress) {
        checkAccountLockout(identifier.toLowerCase());
        checkIpBlocked(ipAddress);
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccessfulLogin(String identifier, String ipAddress,
                                      String userAgent, AccountType accountType) {
        LoginAttempt attempt = LoginAttempt.builder()
                .identifier(identifier.toLowerCase())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .successful(true)
                .accountType(accountType)
                .build();

        loginAttemptRepository.save(attempt);
        log.info("Successful login for: {} from IP: {}", identifier, ipAddress);
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedLogin(String identifier, String ipAddress,
                                  String userAgent, AccountType accountType, String reason) {
        LoginAttempt attempt = LoginAttempt.builder()
                .identifier(identifier.toLowerCase())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .successful(false)
                .failureReason(reason)
                .accountType(accountType)
                .build();

        loginAttemptRepository.save(attempt);

        int failedCount = countRecentFailedAttempts(identifier);
        int remaining = Math.max(0, config.getMaxFailedAttempts() - failedCount);

        log.warn("Failed login for: {} from IP: {}. Reason: {}. Remaining attempts: {}",
                identifier, ipAddress, reason, remaining);
    }

    public int getRemainingAttempts(String identifier) {
        int failed = countRecentFailedAttempts(identifier);
        return Math.max(0, config.getMaxFailedAttempts() - failed);
    }


    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void cleanupOldAttempts() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(config.getAttemptRetentionDays());
        loginAttemptRepository.deleteOldAttempts(cutoff);
        log.info("Cleaned up login attempts older than {}", cutoff);
    }


    private void checkAccountLockout(String identifier) {
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(config.getAttemptWindowMinutes());
        int failedAttempts = loginAttemptRepository.countFailedAttemptsByIdentifier(identifier, windowStart);

        if (failedAttempts < config.getMaxFailedAttempts()) {
            return;
        }

        LocalDateTime lastFailure = loginAttemptRepository
                .findLastFailedAttemptTimeByIdentifier(identifier, windowStart)
                .orElse(null);

        if (lastFailure == null) {
            return;
        }

        LocalDateTime lockoutEnds = lastFailure.plusMinutes(config.getLockoutDurationMinutes());

        if (lockoutEnds.isAfter(LocalDateTime.now())) {
            log.warn("Account locked: {}. Locked until: {}", identifier, lockoutEnds);
            throw new AccountLockedException(lockoutEnds);
        }
        // Lockout period has passed - allow the attempt
    }

    private void checkIpBlocked(String ipAddress) {
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(config.getAttemptWindowMinutes());
        int failedAttempts = loginAttemptRepository.countFailedAttemptsByIp(ipAddress, windowStart);

        if (failedAttempts < config.getMaxFailedAttemptsPerIp()) {
            return;
        }


        LocalDateTime lastFailure = loginAttemptRepository
                .findLastFailedAttemptTimeByIp(ipAddress, windowStart)
                .orElse(null);

        if (lastFailure == null) {
            return;
        }

        LocalDateTime blockEnds = lastFailure.plusMinutes(config.getIpBlockDurationMinutes());

        if (blockEnds.isAfter(LocalDateTime.now())) {
            int remainingMinutes = (int) java.time.Duration.between(LocalDateTime.now(), blockEnds).toMinutes();
            log.warn("IP blocked: {}. Blocked until: {}", ipAddress, blockEnds);
            throw new TooManyRequestsException(Math.max(1, remainingMinutes));
        }
    }

    private int countRecentFailedAttempts(String identifier) {
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(config.getAttemptWindowMinutes());
        return loginAttemptRepository.countFailedAttemptsByIdentifier(identifier.toLowerCase(), windowStart);
    }
}
