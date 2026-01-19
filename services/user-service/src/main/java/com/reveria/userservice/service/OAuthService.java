package com.reveria.userservice.service;

import com.reveria.userservice.dto.OAuthUserInfo;
import com.reveria.userservice.dto.SessionInfo;
import com.reveria.userservice.dto.response.AuthResponse;
import com.reveria.userservice.model.entity.AuthProvider;
import com.reveria.userservice.model.entity.RefreshToken;
import com.reveria.userservice.model.entity.User;
import com.reveria.userservice.model.enums.AccountType;
import com.reveria.userservice.model.enums.ProviderType;
import com.reveria.userservice.model.enums.UserStatus;
import com.reveria.userservice.exception.OAuthException;
import com.reveria.userservice.mapper.AuthMapper;
import com.reveria.userservice.repository.AuthProviderRepository;
import com.reveria.userservice.repository.UserRepository;
import com.reveria.userservice.security.JWTService;
import com.reveria.userservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthService {

    private final UserRepository userRepository;
    private final AuthProviderRepository authProviderRepository;
    private final RefreshTokenService refreshTokenService;
    private final JWTService jwtService;
    private final AuthMapper authMapper;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final RestTemplate restTemplate;

    private final DefaultOAuth2UserService oAuth2UserService = new DefaultOAuth2UserService();

    @Transactional
    public AuthResponse authenticate(ProviderType provider, String code, String redirectUri, SessionInfo sessionInfo) {
        OAuthUserInfo userInfo = getUserInfo(provider, code, redirectUri);
        User user = findOrCreateUser(userInfo);
        return generateAuthResponse(user, sessionInfo);
    }

    private OAuthUserInfo getUserInfo(ProviderType provider, String code, String redirectUri) {
        String registrationId = provider.name().toLowerCase();
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(registrationId);

        if (clientRegistration == null) {
            throw new OAuthException("Provider not configured: " + provider, provider);
        }

        try {
            OAuth2AccessToken accessToken = exchangeCodeForToken(clientRegistration, code, redirectUri);
            OAuth2UserRequest userRequest = new OAuth2UserRequest(clientRegistration, accessToken);
            OAuth2User oAuth2User = oAuth2UserService.loadUser(userRequest);

            return mapToUserInfo(provider, oAuth2User, accessToken);

        } catch (OAuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("OAuth authentication failed for {}", provider, e);
            throw new OAuthException("Authentication failed: " + e.getMessage(), provider, e);
        }
    }

    private OAuth2AccessToken exchangeCodeForToken(ClientRegistration registration, String code, String redirectUri) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("code", code);
        params.add("redirect_uri", redirectUri);
        params.add("client_id", registration.getClientId());
        params.add("client_secret", registration.getClientSecret());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                registration.getProviderDetails().getTokenUri(),
                request,
                Map.class
        );

        Map<String, Object> body = response.getBody();
        if (body == null || !body.containsKey("access_token")) {
            throw new OAuthException("Failed to exchange code for token",
                    ProviderType.valueOf(registration.getRegistrationId().toUpperCase()));
        }

        String tokenValue = (String) body.get("access_token");
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(
                body.containsKey("expires_in") ? ((Number) body.get("expires_in")).longValue() : 3600
        );

        return new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                tokenValue,
                issuedAt,
                expiresAt
        );
    }

    private OAuthUserInfo mapToUserInfo(ProviderType provider, OAuth2User oAuth2User, OAuth2AccessToken accessToken) {
        Map<String, Object> attributes = oAuth2User.getAttributes();

        return switch (provider) {
            case GOOGLE -> OAuthUserInfo.builder()
                    .provider(provider)
                    .providerId((String) attributes.get("sub"))
                    .email((String) attributes.get("email"))
                    .name((String) attributes.get("name"))
                    .pictureUrl((String) attributes.get("picture"))
                    .build();

            case FACEBOOK -> {
                String pictureUrl = null;
                if (attributes.get("picture") instanceof Map picture) {
                    Map<String, Object> data = (Map<String, Object>) picture.get("data");
                    if (data != null) {
                        pictureUrl = (String) data.get("url");
                    }
                }
                yield OAuthUserInfo.builder()
                        .provider(provider)
                        .providerId((String) attributes.get("id"))
                        .email((String) attributes.get("email"))
                        .name((String) attributes.get("name"))
                        .pictureUrl(pictureUrl)
                        .build();
            }

            case GITHUB -> {
                String email = (String) attributes.get("email");

                if (email == null) {
                    email = fetchGitHubEmail(accessToken.getTokenValue());
                }

                yield OAuthUserInfo.builder()
                        .provider(provider)
                        .providerId(String.valueOf(attributes.get("id")))
                        .email(email)
                        .name((String) attributes.get("name"))
                        .pictureUrl((String) attributes.get("avatar_url"))
                        .build();
            }

            default -> throw new OAuthException("Unsupported provider", provider);
        };
    }

    private String fetchGitHubEmail(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    "https://api.github.com/user/emails",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );

            List<Map<String, Object>> emails = response.getBody();
            if (emails != null) {
                for (Map<String, Object> email : emails) {
                    if (Boolean.TRUE.equals(email.get("primary")) && Boolean.TRUE.equals(email.get("verified"))) {
                        return (String) email.get("email");
                    }
                }
                for (Map<String, Object> email : emails) {
                    if (Boolean.TRUE.equals(email.get("verified"))) {
                        return (String) email.get("email");
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch GitHub email", e);
        }

        throw new OAuthException("Could not retrieve email from GitHub", ProviderType.GITHUB);
    }

    private User findOrCreateUser(OAuthUserInfo userInfo) {
        Optional<AuthProvider> existingProvider = authProviderRepository
                .findByProviderAndProviderId(userInfo.getProvider(), userInfo.getProviderId());

        if (existingProvider.isPresent()) {
            User user = existingProvider.get().getUser();
            user.setLastLoginAt(LocalDateTime.now());

            if (userInfo.getPictureUrl() != null) {
                user.setAvatarUrl(userInfo.getPictureUrl());
            }

            return userRepository.save(user);
        }

        Optional<User> existingUser = userRepository.findByEmail(userInfo.getEmail());

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            linkProvider(user, userInfo);
            user.setLastLoginAt(LocalDateTime.now());
            return userRepository.save(user);
        }

        return createNewUser(userInfo);
    }

    private User createNewUser(OAuthUserInfo userInfo) {
        String username = generateUniqueUsername(userInfo.getName(), userInfo.getEmail());

        User user = User.builder()
                .email(userInfo.getEmail().toLowerCase())
                .username(username)
                .displayName(userInfo.getName() != null ? userInfo.getName() : username)
                .avatarUrl(userInfo.getPictureUrl())
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .lastLoginAt(LocalDateTime.now())
                .build();

        user = userRepository.save(user);
        linkProvider(user, userInfo);

        log.info("New OAuth user created: {} via {}", user.getUsername(), userInfo.getProvider());
        return user;
    }

    private void linkProvider(User user, OAuthUserInfo userInfo) {
        AuthProvider provider = AuthProvider.builder()
                .user(user)
                .provider(userInfo.getProvider())
                .providerId(userInfo.getProviderId())
                .linkedAt(LocalDateTime.now())
                .build();

        authProviderRepository.save(provider);
        log.info("Linked {} provider to user: {}", userInfo.getProvider(), user.getUsername());
    }

    private String generateUniqueUsername(String name, String email) {
        String baseUsername;
        if (name != null && !name.isBlank()) {
            String cleanedName = name.toLowerCase()
                    .replaceAll("[^a-z0-9]", "");
            baseUsername = cleanedName
                    .substring(0, Math.min(cleanedName.length(), 15));
        } else {
            String emailPart = email.split("@")[0]
                    .toLowerCase();
            baseUsername = emailPart.substring(0, Math.min(emailPart.length(), 15));
        }

        if (baseUsername.length() < 3) {
            baseUsername = "user";
        }

        String username = baseUsername;
        int counter = 1;

        while (userRepository.existsByUsername(username)) {
            username = baseUsername + counter;
            counter++;
        }

        return username;
    }

    private AuthResponse generateAuthResponse(User user, SessionInfo sessionInfo) {
        UserPrincipal principal = new UserPrincipal(user);

        RefreshToken refreshToken = refreshTokenService.createSession(user, sessionInfo, true);

        String accessToken = jwtService.generateAccessToken(
                principal,
                user.getUuid(),
                AccountType.USER,
                refreshToken.getFamilyId()
        );

        return authMapper.toAuthResponse(
                user,
                accessToken,
                refreshToken.getToken(),
                jwtService.getAccessTokenExpirationInSeconds()
        );
    }

    @Transactional
    public void linkProvider(Long userId, ProviderType provider, String code, String redirectUri) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        OAuthUserInfo userInfo = getUserInfo(provider, code, redirectUri);

        Optional<AuthProvider> existingProvider = authProviderRepository
                .findByProviderAndProviderId(userInfo.getProvider(), userInfo.getProviderId());

        if (existingProvider.isPresent() && !existingProvider.get().getUser().getId().equals(userId)) {
            throw new OAuthException("This account is already linked to another user", provider);
        }

        if (authProviderRepository.existsByUserIdAndProvider(userId, provider)) {
            throw new OAuthException("Provider already linked", provider);
        }

        linkProvider(user, userInfo);
    }

    @Transactional
    public void unlinkProvider(Long userId, ProviderType provider) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean hasPassword = user.getPasswordHash() != null;
        long providerCount = user.getAuthProviders().size();

        if (!hasPassword && providerCount <= 1) {
            throw new IllegalStateException("Cannot unlink the only authentication method");
        }

        user.getAuthProviders().removeIf(p -> p.getProvider() == provider);
        userRepository.save(user);

        log.info("Unlinked {} provider from user: {}", provider, user.getUsername());
    }
}