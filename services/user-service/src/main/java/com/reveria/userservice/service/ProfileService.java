package com.reveria.userservice.service;

import com.reveria.userservice.dto.request.profile.DeactivateAccountRequest;
import com.reveria.userservice.dto.request.profile.UpdatePrivacyRequest;
import com.reveria.userservice.dto.request.profile.UpdateProfileRequest;
import com.reveria.userservice.dto.response.LinkedProviderResponse;
import com.reveria.userservice.dto.response.PrivacySettingsResponse;
import com.reveria.userservice.dto.response.UserProfileResponse;
import com.reveria.userservice.model.entity.User;
import com.reveria.userservice.model.entity.UserPrivacySettings;
import com.reveria.userservice.model.enums.MessagePrivacy;
import com.reveria.userservice.model.enums.ProfileVisibility;
import com.reveria.userservice.model.enums.UserStatus;
import com.reveria.userservice.exception.PasswordMismatchException;
import com.reveria.userservice.exception.UsernameAlreadyExistsException;
import com.reveria.userservice.mapper.UserProfileMapper;
import com.reveria.userservice.repository.UserPrivacySettingsRepository;
import com.reveria.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final UserRepository userRepository;
    private final UserPrivacySettingsRepository privacySettingsRepository;
    private final RefreshTokenService refreshTokenService;
    private final UserProfileMapper profileMapper;
    private final PasswordEncoder passwordEncoder;


    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        User user = findUserById(userId);
        return profileMapper.toProfileResponse(user);
    }


    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUserById(userId);

        if (request.getUsername() != null) {
            String newUsername = request.getUsername().toLowerCase();
            if (!newUsername.equals(user.getUsername())) {
                if (userRepository.existsByUsername(newUsername)) {
                    throw new UsernameAlreadyExistsException(newUsername);
                }
                user.setUsername(newUsername);
            }
        }

        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }

        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }

        user = userRepository.save(user);
        log.info("Profile updated for user: {}", user.getUsername());

        return profileMapper.toProfileResponse(user);
    }


    @Transactional
    public UserProfileResponse updateAvatar(Long userId, String avatarUrl) {
        User user = findUserById(userId);
        user.setAvatarUrl(avatarUrl);
        user = userRepository.save(user);
        log.info("Avatar updated for user: {}", user.getUsername());
        return profileMapper.toProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse deleteAvatar(Long userId) {
        User user = findUserById(userId);
        user.setAvatarUrl(null);
        user = userRepository.save(user);
        log.info("Avatar deleted for user: {}", user.getUsername());
        return profileMapper.toProfileResponse(user);
    }


    @Transactional
    public PrivacySettingsResponse getPrivacySettings(Long userId) {
        UserPrivacySettings settings = getOrCreatePrivacySettings(userId);
        return profileMapper.toPrivacyResponse(settings);
    }

    @Transactional
    public PrivacySettingsResponse updatePrivacySettings(Long userId, UpdatePrivacyRequest request) {
        UserPrivacySettings settings = getOrCreatePrivacySettings(userId);

        if (request.getProfileVisibility() != null) {
            settings.setProfileVisibility(request.getProfileVisibility());
        }

        if (request.getShowOnlineStatus() != null) {
            settings.setShowOnlineStatus(request.getShowOnlineStatus());
        }

        if (request.getAllowDirectMessages() != null) {
            settings.setAllowDirectMessages(request.getAllowDirectMessages());
        }

        if (request.getAllowFriendRequests() != null) {
            settings.setAllowFriendRequests(request.getAllowFriendRequests());
        }

        if (request.getMessagePrivacy() != null) {
            settings.setMessagePrivacy(request.getMessagePrivacy());
        }

        settings = privacySettingsRepository.save(settings);
        log.info("Privacy settings updated for user: {}", userId);

        return profileMapper.toPrivacyResponse(settings);
    }

    private UserPrivacySettings getOrCreatePrivacySettings(Long userId) {
        return privacySettingsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = findUserById(userId);
                    UserPrivacySettings settings = UserPrivacySettings.builder()
                            .user(user)
                            .profileVisibility(ProfileVisibility.PUBLIC)
                            .showOnlineStatus(true)
                            .allowDirectMessages(true)
                            .allowFriendRequests(true)
                            .messagePrivacy(MessagePrivacy.EVERYONE)
                            .build();
                    return privacySettingsRepository.save(settings);
                });
    }


    @Transactional(readOnly = true)
    public List<LinkedProviderResponse> getLinkedProviders(Long userId) {
        User user = findUserById(userId);

        boolean hasPassword = user.getPasswordHash() != null;
        int providerCount = user.getAuthProviders().size();

        boolean canUnlink = hasPassword || providerCount > 1;

        return user.getAuthProviders().stream()
                .map(provider -> profileMapper.toLinkedProviderResponse(
                        provider,
                        canUnlink && providerCount > 1 || hasPassword
                ))
                .toList();
    }

    @Transactional
    public void deactivateAccount(Long userId, DeactivateAccountRequest request) {
        User user = findUserById(userId);

        if (user.getPasswordHash() != null) {
            if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                throw new PasswordMismatchException();
            }
        }

        user.setStatus(UserStatus.DEACTIVATED);
        userRepository.save(user);

        refreshTokenService.revokeAllUserSessions(userId);

        log.info("Account deactivated for user: {}. Reason: {}", user.getUsername(), request.getReason());
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}