package com.reveria.userservice.service;

import com.reveria.userservice.dto.request.moderator.CreateModeratorRequest;
import com.reveria.userservice.dto.response.ModeratorResponse;
import com.reveria.userservice.exception.UsernameAlreadyExistsException;
import com.reveria.userservice.mapper.ModeratorAuthMapper;
import com.reveria.userservice.model.entity.Moderator;
import com.reveria.userservice.model.enums.ModeratorRole;
import com.reveria.userservice.repository.ModeratorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModeratorManagementServiceTest {

    @Mock private ModeratorRepository moderatorRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ModeratorAuthMapper moderatorAuthMapper;
    @Mock private RefreshTokenService refreshTokenService;

    @InjectMocks private ModeratorManagementService moderatorManagementService;

    @Test
    void createModerator_success() {
        CreateModeratorRequest request = buildCreateRequest();
        when(moderatorRepository.existsByUsername("moduser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPass");
        when(moderatorRepository.save(any(Moderator.class))).thenAnswer(inv -> {
            Moderator m = inv.getArgument(0);
            m.setId(1L);
            m.setUuid("mod-uuid");
            return m;
        });
        when(moderatorAuthMapper.toModeratorResponse(any(Moderator.class))).thenReturn(buildModeratorResponse());

        ModeratorResponse response = moderatorManagementService.createModerator(request);

        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("moduser");

        ArgumentCaptor<Moderator> captor = ArgumentCaptor.forClass(Moderator.class);
        verify(moderatorRepository).save(captor.capture());
        Moderator saved = captor.getValue();
        assertThat(saved.getPasswordHash()).isEqualTo("encodedPass");
        assertThat(saved.getUsername()).isEqualTo("moduser");
        assertThat(saved.getRole()).isEqualTo(ModeratorRole.MODERATOR);
    }

    @Test
    void createModerator_duplicateUsername_throws() {
        CreateModeratorRequest request = buildCreateRequest();
        when(moderatorRepository.existsByUsername("moduser")).thenReturn(true);

        assertThatThrownBy(() -> moderatorManagementService.createModerator(request))
                .isInstanceOf(UsernameAlreadyExistsException.class);

        verify(moderatorRepository, never()).save(any());
    }

    @Test
    void listModerators_returnsAll() {
        Moderator m1 = buildModerator("mod1", "uuid-1");
        Moderator m2 = buildModerator("mod2", "uuid-2");
        when(moderatorRepository.findAll()).thenReturn(List.of(m1, m2));
        when(moderatorAuthMapper.toModeratorResponse(m1)).thenReturn(buildModeratorResponse("mod1", "uuid-1"));
        when(moderatorAuthMapper.toModeratorResponse(m2)).thenReturn(buildModeratorResponse("mod2", "uuid-2"));

        List<ModeratorResponse> result = moderatorManagementService.listModerators();

        assertThat(result).hasSize(2);
        verify(moderatorRepository).findAll();
    }

    @Test
    void getModerator_found() {
        Moderator moderator = buildModerator("moduser", "mod-uuid");
        when(moderatorRepository.findByUuid("mod-uuid")).thenReturn(Optional.of(moderator));
        when(moderatorAuthMapper.toModeratorResponse(moderator)).thenReturn(buildModeratorResponse());

        ModeratorResponse response = moderatorManagementService.getModerator("mod-uuid");

        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("moduser");
    }

    @Test
    void getModerator_notFound_throws() {
        when(moderatorRepository.findByUuid("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> moderatorManagementService.getModerator("nonexistent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Moderator not found");
    }

    @Test
    void deactivateModerator_success() {
        Moderator moderator = buildModerator("moduser", "target-uuid");
        moderator.setId(10L);
        moderator.setActive(true);
        when(moderatorRepository.findByUuid("target-uuid")).thenReturn(Optional.of(moderator));
        when(moderatorRepository.save(moderator)).thenReturn(moderator);
        when(moderatorAuthMapper.toModeratorResponse(moderator)).thenReturn(buildModeratorResponse());

        ModeratorResponse response = moderatorManagementService.deactivateModerator("target-uuid", "caller-uuid");

        assertThat(moderator.getActive()).isFalse();
        verify(moderatorRepository).save(moderator);
        verify(refreshTokenService).revokeAllModeratorSessions(10L);
    }

    @Test
    void deactivateModerator_self_throws() {
        assertThatThrownBy(() -> moderatorManagementService.deactivateModerator("same-uuid", "same-uuid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot deactivate your own account");

        verify(moderatorRepository, never()).findByUuid(any());
    }

    // --- Helper builders ---

    private CreateModeratorRequest buildCreateRequest() {
        CreateModeratorRequest request = new CreateModeratorRequest();
        request.setUsername("moduser");
        request.setPassword("password123");
        request.setDisplayName("Mod User");
        request.setRole(ModeratorRole.MODERATOR);
        return request;
    }

    private Moderator buildModerator(String username, String uuid) {
        return Moderator.builder()
                .id(1L)
                .uuid(uuid)
                .username(username)
                .passwordHash("hash")
                .displayName(username)
                .role(ModeratorRole.MODERATOR)
                .active(true)
                .build();
    }

    private ModeratorResponse buildModeratorResponse() {
        return buildModeratorResponse("moduser", "mod-uuid");
    }

    private ModeratorResponse buildModeratorResponse(String username, String uuid) {
        return ModeratorResponse.builder()
                .uuid(uuid)
                .username(username)
                .displayName(username)
                .role(ModeratorRole.MODERATOR)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
