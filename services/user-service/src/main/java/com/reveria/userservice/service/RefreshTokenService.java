package com.reveria.userservice.service;

import com.reveria.userservice.dto.SessionInfo;
import com.reveria.userservice.model.entity.Moderator;
import com.reveria.userservice.model.entity.RefreshToken;
import com.reveria.userservice.model.entity.User;
import com.reveria.userservice.model.enums.AccountType;
import com.reveria.userservice.exception.JwtAuthenticationException;
import com.reveria.userservice.exception.TokenReuseException;
import com.reveria.userservice.repository.RefreshTokenRepository;
import com.reveria.userservice.security.JWTService;
import com.reveria.userservice.security.ModeratorPrincipal;
import com.reveria.userservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {


    private final RefreshTokenRepository refreshTokenRepository;
    private final JWTService jwtService;

    @Value("${security.max-sessions-per-user:5}")
    private int maxSessionsPerUser;

    @Value("${security.max-sessions-per-moderator:3}")
    private int maxSessionsPerModerator;

    private final PlatformTransactionManager transactionManager;


    //CREATE SESSION

    @Transactional
    public RefreshToken createSession(User user, SessionInfo sessionInfo, boolean rememberMe) {
        enforceSessionLimit(user.getId(), AccountType.USER);

        String familyId = jwtService.generateNewFamilyId();
        UserPrincipal principal = new UserPrincipal(user);

        long expirationSeconds = rememberMe
                ? jwtService.getRefreshTokenExpirationInSeconds()
                : 3600;

        String token = jwtService.generateRefreshToken(
                principal, user.getUuid(), AccountType.USER, familyId, 1
        );

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .familyId(familyId)
                .generation(1)
                .ipAddress(sessionInfo.ipAddress())
                .userAgent(sessionInfo.userAgent())
                .accountType(AccountType.USER)
                .expiresAt(LocalDateTime.now().plusSeconds(expirationSeconds))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public RefreshToken createSession(Moderator moderator, SessionInfo sessionInfo) {
        enforceSessionLimit(moderator.getId(), AccountType.MODERATOR);

        String familyId = jwtService.generateNewFamilyId();
        ModeratorPrincipal principal = new ModeratorPrincipal(moderator);

        String token = jwtService.generateRefreshToken(
                principal, moderator.getUuid(), AccountType.MODERATOR, familyId, 1
        );

        RefreshToken refreshToken = RefreshToken.builder()
                .moderator(moderator)
                .token(token)
                .familyId(familyId)
                .generation(1)
                .ipAddress(sessionInfo.ipAddress())
                .userAgent(sessionInfo.userAgent())
                .accountType(AccountType.MODERATOR)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtService.getRefreshTokenExpirationInSeconds()))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    //SESSION LIMIT

    private void enforceSessionLimit(Long accountId, AccountType accountType) {
        int maxSessions = accountType == AccountType.USER
                ? maxSessionsPerUser
                : maxSessionsPerModerator;

        int activeSessions = accountType == AccountType.USER
                ? refreshTokenRepository.countActiveSessionsByUserId(accountId, LocalDateTime.now())
                : refreshTokenRepository.countActiveSessionsByModeratorId(accountId, LocalDateTime.now());

        if (activeSessions >= maxSessions) {
            log.info("Max sessions reached for {} {}, revoking oldest", accountType, accountId);
            revokeOldestSession(accountId, accountType);
        }
    }

    private void revokeOldestSession(Long accountId, AccountType accountType) {
        List<RefreshToken> sessions = accountType == AccountType.USER
                ? refreshTokenRepository.findActiveSessionsByUserId(accountId, LocalDateTime.now())
                : refreshTokenRepository.findActiveSessionsByModeratorId(accountId, LocalDateTime.now());

        sessions.stream()
                .min(Comparator.comparing(RefreshToken::getCreatedAt))
                .ifPresent(oldest -> {
                    log.info("Revoking oldest session family: {}", oldest.getFamilyId());
                    refreshTokenRepository.revokeFamily(oldest.getFamilyId());
                });
    }

    //ROTATE TOKEN
    @Transactional
    public RefreshToken rotateToken(String oldTokenString, SessionInfo sessionInfo) {
        RefreshToken oldToken = refreshTokenRepository.findByToken(oldTokenString)
                .orElseThrow(() -> new JwtAuthenticationException("Refresh token not found"));

        if (oldToken.getRevoked()) {
            throw new JwtAuthenticationException("Refresh token has been revoked");
        }
        // REUSE DETECTION
        if (oldToken.getUsed()) {
            log.warn("TOKEN REUSE DETECTED! Family: {}", oldToken.getFamilyId());
            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            transactionTemplate.execute(status -> {
                log.warn("Revoking family {} due to token reuse", oldToken.getFamilyId());
                refreshTokenRepository.revokeFamily(oldToken.getFamilyId());
                return null;
            });
            throw new TokenReuseException(oldToken.getFamilyId());
        }

        if(oldToken.isExpired()) {
            throw new JwtAuthenticationException("Refresh token has expired");
        }

        oldToken.setUsed(true);
        oldToken.setUsedAt(LocalDateTime.now());

        int newGeneration = oldToken.getGeneration() + 1;
        RefreshToken newToken;

        if (oldToken.getAccountType() == AccountType.USER) {
            User user = oldToken.getUser();
            String newTokenString = jwtService.generateRefreshToken(
                    new UserPrincipal(user), user.getUuid(), AccountType.USER,
                    oldToken.getFamilyId(), newGeneration
            );

            newToken = RefreshToken.builder()
                    .user(user)
                    .token(newTokenString)
                    .familyId(oldToken.getFamilyId())
                    .generation(newGeneration)
                    .ipAddress(sessionInfo.ipAddress())
                    .userAgent(sessionInfo.userAgent())
                    .accountType(AccountType.USER)
                    .expiresAt(LocalDateTime.now().plusSeconds(jwtService.getRefreshTokenExpirationInSeconds()))
                    .build();
        } else {
            Moderator moderator = oldToken.getModerator();
            String newTokenString = jwtService.generateRefreshToken(
                    new ModeratorPrincipal(moderator), moderator.getUuid(), AccountType.MODERATOR,
                    oldToken.getFamilyId(), newGeneration
            );

            newToken = RefreshToken.builder()
                    .moderator(moderator)
                    .token(newTokenString)
                    .familyId(oldToken.getFamilyId())
                    .generation(newGeneration)
                    .ipAddress(sessionInfo.ipAddress())
                    .userAgent(sessionInfo.userAgent())
                    .accountType(AccountType.MODERATOR)
                    .expiresAt(LocalDateTime.now().plusSeconds(jwtService.getRefreshTokenExpirationInSeconds()))
                    .build();
        }

        oldToken.setReplacedByToken(newToken.getToken());
        refreshTokenRepository.save(oldToken);

        return refreshTokenRepository.save(newToken);
    }

    //VALIDATION

    public RefreshToken validateAndGet(String token) {
        jwtService.validateRefreshToken(token);

        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new JwtAuthenticationException("Refresh token not found"));

        if (!refreshToken.isValid()) {
            if (refreshToken.getUsed()) {
                refreshTokenRepository.revokeFamily(refreshToken.getFamilyId());
                throw new TokenReuseException(refreshToken.getFamilyId());
            }
            throw new JwtAuthenticationException("Refresh token is invalid");
        }

        return refreshToken;
    }

    //REVOCATION

    @Transactional
    public void revokeSession(String familyId) {
        log.info("Revoking session family: {}", familyId);
        refreshTokenRepository.revokeFamily(familyId);
    }

    @Transactional
    public void revokeAllUserSessions(Long userId) {
        log.info("Revoking all sessions for user: {}", userId);
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    @Transactional
    public void revokeAllModeratorSessions(Long moderatorId) {
        log.info("Revoking all sessions for moderator: {}", moderatorId);
        refreshTokenRepository.revokeAllByModeratorId(moderatorId);
    }

    //QUERIES (for "Manage Devices" UI)

    public List<RefreshToken> getActiveSessions(Long userId) {
        return refreshTokenRepository.findActiveSessionsByUserId(userId, LocalDateTime.now());
    }

    public int getActiveSessionCount(Long userId) {
        return refreshTokenRepository.countActiveSessionsByUserId(userId, LocalDateTime.now());
    }

    //CLEANUP
    @Scheduled(cron = "0 0 */6 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Cleaning up expired refresh tokens");
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }
}