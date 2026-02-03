package com.reveria.userservice.service;

import com.reveria.userservice.dto.response.UserProfileResponse;
import com.reveria.userservice.mapper.UserProfileMapper;
import com.reveria.userservice.model.entity.User;
import com.reveria.userservice.model.enums.UserStatus;
import com.reveria.userservice.repository.UserPrivacySettingsRepository;
import com.reveria.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.InputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceAvatarTest {

    @Mock private UserRepository userRepository;
    @Mock private UserPrivacySettingsRepository privacySettingsRepository;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private UserProfileMapper profileMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private StorageService storageService;
    @Mock private UserEventPublisher userEventPublisher;

    @InjectMocks private ProfileService profileService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .uuid("user-uuid-123")
                .email("test@example.com")
                .username("testuser")
                .displayName("Test User")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();
    }

    @Test
    void updateAvatar_uploadsSavesUrl() {
        byte[] jpegBytes = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00};
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", jpegBytes);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(storageService.upload(anyString(), any(InputStream.class), eq((long) jpegBytes.length), eq("image/jpeg")))
                .thenReturn("http://localhost:9000/reveria-avatars/avatars/user-uuid-123/random.jpg");
        when(userRepository.save(user)).thenReturn(user);
        when(profileMapper.toProfileResponse(user)).thenReturn(UserProfileResponse.builder().build());

        profileService.updateAvatar(1L, file);

        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        verify(storageService).upload(pathCaptor.capture(), any(InputStream.class), eq((long) jpegBytes.length), eq("image/jpeg"));

        String uploadedPath = pathCaptor.getValue();
        assertThat(uploadedPath).startsWith("avatars/user-uuid-123/");
        assertThat(uploadedPath).endsWith(".jpg");
        assertThat(user.getAvatarUrl()).isEqualTo("http://localhost:9000/reveria-avatars/avatars/user-uuid-123/random.jpg");
        verify(userRepository).save(user);
    }

    @Test
    void updateAvatar_deletesOldStoredAvatar() {
        String oldUrl = "http://localhost:9000/reveria-avatars/avatars/user-uuid-123/old.jpg";
        String oldPath = "avatars/user-uuid-123/old.jpg";
        user.setAvatarUrl(oldUrl);

        byte[] jpegBytes = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00};
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", jpegBytes);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(storageService.extractPathFromUrl(oldUrl)).thenReturn(oldPath);
        when(storageService.upload(anyString(), any(InputStream.class), anyLong(), anyString()))
                .thenReturn("http://localhost:9000/reveria-avatars/avatars/user-uuid-123/new.jpg");
        when(userRepository.save(user)).thenReturn(user);
        when(profileMapper.toProfileResponse(user)).thenReturn(UserProfileResponse.builder().build());

        profileService.updateAvatar(1L, file);

        verify(storageService).delete(oldPath);
    }

    @Test
    void updateAvatar_doesNotDeleteOAuthAvatar() {
        String oauthUrl = "https://lh3.googleusercontent.com/a/avatar123";
        user.setAvatarUrl(oauthUrl);

        byte[] jpegBytes = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00};
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", jpegBytes);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(storageService.extractPathFromUrl(oauthUrl)).thenReturn(null);
        when(storageService.upload(anyString(), any(InputStream.class), anyLong(), anyString()))
                .thenReturn("http://localhost:9000/reveria-avatars/avatars/user-uuid-123/new.jpg");
        when(userRepository.save(user)).thenReturn(user);
        when(profileMapper.toProfileResponse(user)).thenReturn(UserProfileResponse.builder().build());

        profileService.updateAvatar(1L, file);

        verify(storageService, never()).delete(anyString());
    }

    @Test
    void deleteAvatar_removesStoredFile() {
        String storedUrl = "http://localhost:9000/reveria-avatars/avatars/user-uuid-123/avatar.jpg";
        String storedPath = "avatars/user-uuid-123/avatar.jpg";
        user.setAvatarUrl(storedUrl);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(storageService.extractPathFromUrl(storedUrl)).thenReturn(storedPath);
        when(userRepository.save(user)).thenReturn(user);
        when(profileMapper.toProfileResponse(user)).thenReturn(UserProfileResponse.builder().build());

        profileService.deleteAvatar(1L);

        verify(storageService).delete(storedPath);
        assertThat(user.getAvatarUrl()).isNull();
    }

    @Test
    void deleteAvatar_oauthUrl_doesNotDeleteFromStorage() {
        String oauthUrl = "https://lh3.googleusercontent.com/a/avatar123";
        user.setAvatarUrl(oauthUrl);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(storageService.extractPathFromUrl(oauthUrl)).thenReturn(null);
        when(userRepository.save(user)).thenReturn(user);
        when(profileMapper.toProfileResponse(user)).thenReturn(UserProfileResponse.builder().build());

        profileService.deleteAvatar(1L);

        verify(storageService, never()).delete(anyString());
        assertThat(user.getAvatarUrl()).isNull();
    }
}
