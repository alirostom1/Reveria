package com.reveria.userservice.service;

import com.reveria.userservice.dto.SessionInfo;
import com.reveria.userservice.dto.request.auth.LoginRequest;
import com.reveria.userservice.dto.request.auth.RegisterRequest;
import com.reveria.userservice.dto.response.AuthResponse;
import com.reveria.userservice.exception.EmailAlreadyExistsException;
import com.reveria.userservice.exception.PasswordMismatchException;
import com.reveria.userservice.exception.UsernameAlreadyExistsException;
import com.reveria.userservice.mapper.AuthMapper;
import com.reveria.userservice.mapper.SessionMapper;
import com.reveria.userservice.mapper.UserMapper;
import com.reveria.userservice.model.entity.RefreshToken;
import com.reveria.userservice.model.entity.User;
import com.reveria.userservice.model.enums.AccountType;
import com.reveria.userservice.model.enums.UserStatus;
import com.reveria.userservice.repository.UserRepository;
import com.reveria.userservice.security.JWTService;
import com.reveria.userservice.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private JWTService jwtService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserMapper userMapper;
    @Mock private AuthMapper authMapper;
    @Mock private SessionMapper sessionMapper;
    @Mock private EmailVerificationService emailVerificationService;
    @Mock private LoginAttemptService loginAttemptService;
    @Mock private UserEventPublisher userEventPublisher;

    @InjectMocks private AuthService authService;

    private SessionInfo sessionInfo;

    @BeforeEach
    void setUp() {
        sessionInfo = new SessionInfo("127.0.0.1", "TestBrowser/1.0");
    }

    @Test
    void register_success() {
        RegisterRequest request = buildRegisterRequest();
        User mappedUser = new User();
        User savedUser = buildUser();

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(mappedUser);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(mappedUser)).thenReturn(savedUser);
        when(refreshTokenService.createSession(eq(savedUser), eq(sessionInfo), eq(false)))
                .thenReturn(buildRefreshToken(savedUser));
        when(jwtService.generateAccessToken(any(UserPrincipal.class), eq("uuid-123"), eq(AccountType.USER), eq("family-1")))
                .thenReturn("access-token");
        when(jwtService.getAccessTokenExpirationInSeconds()).thenReturn(3600L);
        when(authMapper.toAuthResponse(eq(savedUser), eq("access-token"), eq("refresh-token-value"), eq(3600L)))
                .thenReturn(buildAuthResponse());

        AuthResponse response = authService.register(request, sessionInfo);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        verify(userRepository).save(mappedUser);
        verify(passwordEncoder).encode("password123");
        verify(emailVerificationService).sendVerificationEmail(savedUser);
    }

    @Test
    void register_duplicateEmail_throws() {
        RegisterRequest request = buildRegisterRequest();
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request, sessionInfo))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_duplicateUsername_throws() {
        RegisterRequest request = buildRegisterRequest();
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request, sessionInfo))
                .isInstanceOf(UsernameAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_success() {
        LoginRequest request = buildLoginRequest();
        User user = buildUser();
        UserPrincipal principal = new UserPrincipal(user);
        Authentication auth = mock(Authentication.class);

        when(auth.getPrincipal()).thenReturn(principal);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(userRepository.save(user)).thenReturn(user);
        when(refreshTokenService.createSession(eq(user), eq(sessionInfo), eq(false)))
                .thenReturn(buildRefreshToken(user));
        when(jwtService.generateAccessToken(any(UserPrincipal.class), eq("uuid-123"), eq(AccountType.USER), eq("family-1")))
                .thenReturn("access-token");
        when(jwtService.getAccessTokenExpirationInSeconds()).thenReturn(3600L);
        when(authMapper.toAuthResponse(eq(user), eq("access-token"), eq("refresh-token-value"), eq(3600L)))
                .thenReturn(buildAuthResponse());

        AuthResponse response = authService.login(request, sessionInfo);

        assertThat(response).isNotNull();
        verify(loginAttemptService).checkLoginAllowed("testuser", "127.0.0.1");
        verify(loginAttemptService).recordSuccessfulLogin("testuser", "127.0.0.1", "TestBrowser/1.0", AccountType.USER);
        verify(userRepository).save(user);
    }

    @Test
    void login_invalidCredentials_recordsFailure() {
        LoginRequest request = buildLoginRequest();
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request, sessionInfo))
                .isInstanceOf(BadCredentialsException.class);

        verify(loginAttemptService).recordFailedLogin("testuser", "127.0.0.1", "TestBrowser/1.0", AccountType.USER, "Invalid credentials");
    }

    @Test
    void refresh_success() {
        User user = buildUser();
        RefreshToken newToken = buildRefreshToken(user);
        newToken.setGeneration(2);

        when(refreshTokenService.rotateToken("old-refresh-token", sessionInfo)).thenReturn(newToken);
        when(jwtService.generateAccessToken(any(UserPrincipal.class), eq("uuid-123"), eq(AccountType.USER), eq("family-1")))
                .thenReturn("new-access-token");
        when(jwtService.getAccessTokenExpirationInSeconds()).thenReturn(3600L);

        AuthResponse response = authService.refresh("old-refresh-token", sessionInfo);

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token-value");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    void logout_revokesSession() {
        authService.logout("family-1");

        verify(refreshTokenService).revokeSession("family-1");
    }

    @Test
    void changePassword_success() {
        User user = buildUser();
        user.setPasswordHash("existingHash");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("currentPass", "existingHash")).thenReturn(true);
        when(passwordEncoder.matches("newPass", "existingHash")).thenReturn(false);
        when(passwordEncoder.encode("newPass")).thenReturn("newEncodedHash");

        authService.changePassword(1L, "family-1", "currentPass", "newPass", false);

        assertThat(user.getPasswordHash()).isEqualTo("newEncodedHash");
        verify(userRepository).save(user);
        verify(refreshTokenService, never()).getActiveSessions(anyLong());
    }

    // --- Helper builders ---

    private RegisterRequest buildRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setUsername("testuser");
        request.setPassword("password123");
        request.setDisplayName("Test User");
        return request;
    }

    private LoginRequest buildLoginRequest() {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("testuser");
        request.setPassword("password123");
        request.setRememberMe(false);
        return request;
    }

    private User buildUser() {
        return User.builder()
                .id(1L)
                .uuid("uuid-123")
                .email("test@example.com")
                .username("testuser")
                .displayName("Test User")
                .passwordHash("encodedPassword")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();
    }

    private RefreshToken buildRefreshToken(User user) {
        return RefreshToken.builder()
                .id(1L)
                .user(user)
                .token("refresh-token-value")
                .familyId("family-1")
                .generation(1)
                .accountType(AccountType.USER)
                .build();
    }

    private AuthResponse buildAuthResponse() {
        return AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token-value")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .build();
    }
}
