package com.reveria.userservice.service;

import com.reveria.userservice.config.RateLimitConfig;
import com.reveria.userservice.exception.AccountLockedException;
import com.reveria.userservice.exception.TooManyRequestsException;
import com.reveria.userservice.model.entity.LoginAttempt;
import com.reveria.userservice.model.enums.AccountType;
import com.reveria.userservice.repository.LoginAttemptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    @Mock private LoginAttemptRepository loginAttemptRepository;
    @Mock private RateLimitConfig config;

    @InjectMocks private LoginAttemptService loginAttemptService;

    @BeforeEach
    void setUp() {
        lenient().when(config.getMaxFailedAttempts()).thenReturn(5);
        lenient().when(config.getMaxFailedAttemptsPerIp()).thenReturn(20);
        lenient().when(config.getLockoutDurationMinutes()).thenReturn(15);
        lenient().when(config.getIpBlockDurationMinutes()).thenReturn(30);
        lenient().when(config.getAttemptWindowMinutes()).thenReturn(60);
    }

    @Test
    void checkLoginAllowed_noFailures_passes() {
        when(loginAttemptRepository.countFailedAttemptsByIdentifier(eq("user@test.com"), any(LocalDateTime.class)))
                .thenReturn(0);
        when(loginAttemptRepository.countFailedAttemptsByIp(eq("10.0.0.1"), any(LocalDateTime.class)))
                .thenReturn(0);

        loginAttemptService.checkLoginAllowed("user@test.com", "10.0.0.1");

        // No exception thrown — passes
    }

    @Test
    void checkLoginAllowed_accountLocked_throws() {
        when(loginAttemptRepository.countFailedAttemptsByIdentifier(eq("user@test.com"), any(LocalDateTime.class)))
                .thenReturn(5);
        when(loginAttemptRepository.findLastFailedAttemptTimeByIdentifier(eq("user@test.com"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(LocalDateTime.now().minusMinutes(2)));

        assertThatThrownBy(() -> loginAttemptService.checkLoginAllowed("user@test.com", "10.0.0.1"))
                .isInstanceOf(AccountLockedException.class);
    }

    @Test
    void checkLoginAllowed_ipBlocked_throws() {
        when(loginAttemptRepository.countFailedAttemptsByIdentifier(eq("user@test.com"), any(LocalDateTime.class)))
                .thenReturn(0);
        when(loginAttemptRepository.countFailedAttemptsByIp(eq("10.0.0.1"), any(LocalDateTime.class)))
                .thenReturn(20);
        when(loginAttemptRepository.findLastFailedAttemptTimeByIp(eq("10.0.0.1"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(LocalDateTime.now().minusMinutes(2)));

        assertThatThrownBy(() -> loginAttemptService.checkLoginAllowed("user@test.com", "10.0.0.1"))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void recordSuccessfulLogin_savesAttempt() {
        loginAttemptService.recordSuccessfulLogin("user@test.com", "10.0.0.1", "Chrome", AccountType.USER);

        ArgumentCaptor<LoginAttempt> captor = ArgumentCaptor.forClass(LoginAttempt.class);
        verify(loginAttemptRepository).save(captor.capture());

        LoginAttempt saved = captor.getValue();
        assertThat(saved.getIdentifier()).isEqualTo("user@test.com");
        assertThat(saved.getIpAddress()).isEqualTo("10.0.0.1");
        assertThat(saved.getSuccessful()).isTrue();
        assertThat(saved.getAccountType()).isEqualTo(AccountType.USER);
    }

    @Test
    void recordFailedLogin_savesAttemptWithReason() {
        when(loginAttemptRepository.countFailedAttemptsByIdentifier(eq("user@test.com"), any(LocalDateTime.class)))
                .thenReturn(1);

        loginAttemptService.recordFailedLogin("user@test.com", "10.0.0.1", "Chrome", AccountType.USER, "Invalid credentials");

        ArgumentCaptor<LoginAttempt> captor = ArgumentCaptor.forClass(LoginAttempt.class);
        verify(loginAttemptRepository).save(captor.capture());

        LoginAttempt saved = captor.getValue();
        assertThat(saved.getSuccessful()).isFalse();
        assertThat(saved.getFailureReason()).isEqualTo("Invalid credentials");
    }

    @Test
    void getRemainingAttempts_returnsCorrectCount() {
        when(loginAttemptRepository.countFailedAttemptsByIdentifier(eq("user@test.com"), any(LocalDateTime.class)))
                .thenReturn(3);

        int remaining = loginAttemptService.getRemainingAttempts("user@test.com");

        assertThat(remaining).isEqualTo(2);
    }

    @Test
    void getRemainingAttempts_neverNegative() {
        when(loginAttemptRepository.countFailedAttemptsByIdentifier(eq("user@test.com"), any(LocalDateTime.class)))
                .thenReturn(10);

        int remaining = loginAttemptService.getRemainingAttempts("user@test.com");

        assertThat(remaining).isZero();
    }
}
