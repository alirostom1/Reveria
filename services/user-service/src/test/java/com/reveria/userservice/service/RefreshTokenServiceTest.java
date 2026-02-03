package com.reveria.userservice.service;

import com.reveria.userservice.dto.SessionInfo;
import com.reveria.userservice.exception.JwtAuthenticationException;
import com.reveria.userservice.exception.TokenReuseException;
import com.reveria.userservice.model.entity.RefreshToken;
import com.reveria.userservice.model.entity.User;
import com.reveria.userservice.model.enums.AccountType;
import com.reveria.userservice.model.enums.UserStatus;
import com.reveria.userservice.repository.RefreshTokenRepository;
import com.reveria.userservice.security.JWTService;
import com.reveria.userservice.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JWTService jwtService;
    @Mock private PlatformTransactionManager transactionManager;

    @InjectMocks private RefreshTokenService refreshTokenService;

    private User user;
    private SessionInfo sessionInfo;

    @BeforeEach
    void setUp() throws Exception {
        user = User.builder()
                .id(1L)
                .uuid("uuid-123")
                .email("test@example.com")
                .username("testuser")
                .passwordHash("hash")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();

        sessionInfo = new SessionInfo("127.0.0.1", "TestBrowser/1.0");

        setField("maxSessionsPerUser", 5);
        setField("maxSessionsPerModerator", 3);
    }

    @Test
    void createSession_success() {
        when(refreshTokenRepository.countActiveSessionsByUserId(eq(1L), any(LocalDateTime.class))).thenReturn(0);
        when(jwtService.generateNewFamilyId()).thenReturn("new-family");
        when(jwtService.generateRefreshToken(any(UserPrincipal.class), eq("uuid-123"), eq(AccountType.USER), eq("new-family"), eq(1)))
                .thenReturn("new-refresh-token");
        when(jwtService.getRefreshTokenExpirationInSeconds()).thenReturn(86400L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken result = refreshTokenService.createSession(user, sessionInfo, true);

        assertThat(result.getFamilyId()).isEqualTo("new-family");
        assertThat(result.getGeneration()).isEqualTo(1);
        assertThat(result.getToken()).isEqualTo("new-refresh-token");
        assertThat(result.getAccountType()).isEqualTo(AccountType.USER);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void createSession_exceedsMax_revokesOldest() {
        when(refreshTokenRepository.countActiveSessionsByUserId(eq(1L), any(LocalDateTime.class))).thenReturn(5);

        RefreshToken oldest = RefreshToken.builder()
                .id(1L)
                .familyId("oldest-family")
                .createdAt(LocalDateTime.now().minusDays(7))
                .build();
        RefreshToken newer = RefreshToken.builder()
                .id(2L)
                .familyId("newer-family")
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        when(refreshTokenRepository.findActiveSessionsByUserId(eq(1L), any(LocalDateTime.class)))
                .thenReturn(List.of(oldest, newer));
        when(jwtService.generateNewFamilyId()).thenReturn("new-family");
        when(jwtService.generateRefreshToken(any(UserPrincipal.class), eq("uuid-123"), eq(AccountType.USER), eq("new-family"), eq(1)))
                .thenReturn("token");
        when(jwtService.getRefreshTokenExpirationInSeconds()).thenReturn(86400L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        refreshTokenService.createSession(user, sessionInfo, true);

        verify(refreshTokenRepository).revokeFamily("oldest-family");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void rotateToken_success() {
        RefreshToken oldToken = buildActiveToken();
        when(refreshTokenRepository.findByToken("old-token")).thenReturn(Optional.of(oldToken));
        when(jwtService.generateRefreshToken(any(UserPrincipal.class), eq("uuid-123"), eq(AccountType.USER), eq("family-1"), eq(2)))
                .thenReturn("rotated-token");
        when(jwtService.getRefreshTokenExpirationInSeconds()).thenReturn(86400L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken result = refreshTokenService.rotateToken("old-token", sessionInfo);

        assertThat(result.getToken()).isEqualTo("rotated-token");
        assertThat(result.getGeneration()).isEqualTo(2);
        assertThat(result.getFamilyId()).isEqualTo("family-1");
        assertThat(oldToken.getUsed()).isTrue();
        // save is called twice: once for oldToken, once for newToken
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void rotateToken_reusedToken_revokesFamily() {
        RefreshToken usedToken = buildActiveToken();
        usedToken.setUsed(true);

        when(refreshTokenRepository.findByToken("used-token")).thenReturn(Optional.of(usedToken));
        // Allow the TransactionTemplate to execute its callback
        when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));

        assertThatThrownBy(() -> refreshTokenService.rotateToken("used-token", sessionInfo))
                .isInstanceOf(TokenReuseException.class);

        verify(refreshTokenRepository).revokeFamily("family-1");
    }

    @Test
    void rotateToken_revokedToken_throws() {
        RefreshToken revokedToken = buildActiveToken();
        revokedToken.setRevoked(true);

        when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(revokedToken));

        assertThatThrownBy(() -> refreshTokenService.rotateToken("revoked-token", sessionInfo))
                .isInstanceOf(JwtAuthenticationException.class)
                .hasMessageContaining("revoked");
    }

    @Test
    void revokeSession_revokesAllInFamily() {
        refreshTokenService.revokeSession("family-1");

        verify(refreshTokenRepository).revokeFamily("family-1");
    }

    @Test
    void revokeAllUserSessions_revokesAll() {
        refreshTokenService.revokeAllUserSessions(1L);

        verify(refreshTokenRepository).revokeAllByUserId(1L);
    }

    // --- Helper methods ---

    private RefreshToken buildActiveToken() {
        return RefreshToken.builder()
                .id(1L)
                .user(user)
                .token("old-token")
                .familyId("family-1")
                .generation(1)
                .accountType(AccountType.USER)
                .revoked(false)
                .used(false)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
    }

    private void setField(String fieldName, Object value) throws Exception {
        Field field = RefreshTokenService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(refreshTokenService, value);
    }
}
