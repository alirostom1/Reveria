package com.reveria.userservice.service;

import com.reveria.userservice.dto.request.profile.DeactivateAccountRequest;
import com.reveria.userservice.dto.request.profile.UpdatePrivacyRequest;
import com.reveria.userservice.dto.request.profile.UpdateProfileRequest;
import com.reveria.userservice.dto.response.LinkedProviderResponse;
import com.reveria.userservice.dto.response.PrivacySettingsResponse;
import com.reveria.userservice.dto.response.UserProfileResponse;
import com.reveria.userservice.exception.StorageException;
import com.reveria.userservice.model.entity.User;
import com.reveria.userservice.model.entity.UserPrivacySettings;
import com.reveria.userservice.model.enums.MessagePrivacy;
import com.reveria.userservice.model.enums.ProfileVisibility;
import com.reveria.userservice.model.enums.UserEventType;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final UserRepository userRepository;
    private final UserPrivacySettingsRepository privacySettingsRepository;
    private final RefreshTokenService refreshTokenService;
    private final UserProfileMapper profileMapper;
    private final PasswordEncoder passwordEncoder;
    private final StorageService storageService;
    private final UserEventPublisher userEventPublisher;


    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        User user = findUserById(userId);
        return profileMapper.toProfileResponse(user);
    }


    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUserById(userId);
        List<String> changedFields = new ArrayList<>();

        if (request.getUsername() != null) {
            String newUsername = request.getUsername().toLowerCase();
            if (!newUsername.equals(user.getUsername())) {
                if (userRepository.existsByUsername(newUsername)) {
                    throw new UsernameAlreadyExistsException(newUsername);
                }
                user.setUsername(newUsername);
                changedFields.add("username");
            }
        }

        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
            changedFields.add("displayName");
        }

        if (request.getBio() != null) {
            user.setBio(request.getBio());
            changedFields.add("bio");
        }

        user = userRepository.save(user);
        log.info("Profile updated for user: {}", user.getUsername());
        userEventPublisher.publish(UserEventType.USER_PROFILE_UPDATED, user.getUuid(),
                Map.of("changedFields", changedFields));

        return profileMapper.toProfileResponse(user);
    }


    @Transactional
    public UserProfileResponse updateAvatar(Long userId, MultipartFile file) {
        User user = findUserById(userId);

        deleteStoredAvatar(user.getAvatarUrl());

        String extension = getExtension(file.getContentType());
        String path = "avatars/" + user.getUuid() + "/" + UUID.randomUUID() + "." + extension;

        try {
            String url = storageService.upload(path, file.getInputStream(), file.getSize(), file.getContentType());
            user.setAvatarUrl(url);
        } catch (IOException e) {
            throw new StorageException("Failed to read uploaded file", e);
        }

        user = userRepository.save(user);
        log.info("Avatar updated for user: {}", user.getUsername());
        userEventPublisher.publish(UserEventType.USER_AVATAR_UPDATED, user.getUuid(),
                Map.of("avatarUrl", user.getAvatarUrl()));
        return profileMapper.toProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse deleteAvatar(Long userId) {
        User user = findUserById(userId);

        deleteStoredAvatar(user.getAvatarUrl());

        user.setAvatarUrl(null);
        user = userRepository.save(user);
        log.info("Avatar deleted for user: {}", user.getUsername());
        userEventPublisher.publish(UserEventType.USER_AVATAR_DELETED, user.getUuid(), null);
        return profileMapper.toProfileResponse(user);
    }

    private void deleteStoredAvatar(String avatarUrl) {
        if (avatarUrl == null) {
            return;
        }
        String path = storageService.extractPathFromUrl(avatarUrl);
        if (path != null) {
            storageService.delete(path);
        }
    }

    private static String getExtension(String contentType) {
        return switch (contentType) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }


    @Transactional
    public PrivacySettingsResponse getPrivacySettings(Long userId) {
        UserPrivacySettings settings = getOrCreatePrivacySettings(userId);
        return profileMapper.toPrivacyResponse(settings);
    }

    @Transactional
    public PrivacySettingsResponse updatePrivacySettings(Long userId, UpdatePrivacyRequest request) {
        UserPrivacySettings settings = getOrCreatePrivacySettings(userId);
        Map<String, Object> changedSettings = new HashMap<>();

        if (request.getProfileVisibility() != null) {
            settings.setProfileVisibility(request.getProfileVisibility());
            changedSettings.put("profileVisibility", request.getProfileVisibility().name());
        }

        if (request.getShowOnlineStatus() != null) {
            settings.setShowOnlineStatus(request.getShowOnlineStatus());
            changedSettings.put("showOnlineStatus", request.getShowOnlineStatus());
        }

        if (request.getAllowDirectMessages() != null) {
            settings.setAllowDirectMessages(request.getAllowDirectMessages());
            changedSettings.put("allowDirectMessages", request.getAllowDirectMessages());
        }

        if (request.getAllowFriendRequests() != null) {
            settings.setAllowFriendRequests(request.getAllowFriendRequests());
            changedSettings.put("allowFriendRequests", request.getAllowFriendRequests());
        }

        if (request.getMessagePrivacy() != null) {
            settings.setMessagePrivacy(request.getMessagePrivacy());
            changedSettings.put("messagePrivacy", request.getMessagePrivacy().name());
        }

        settings = privacySettingsRepository.save(settings);
        log.info("Privacy settings updated for user: {}", userId);
        User user = findUserById(userId);
        userEventPublisher.publish(UserEventType.USER_PRIVACY_UPDATED, user.getUuid(), changedSettings);

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
        userEventPublisher.publish(UserEventType.USER_DEACTIVATED, user.getUuid(), null);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}