package com.reveria.userservice.service;

import com.reveria.userservice.dto.request.moderator.CreateModeratorRequest;
import com.reveria.userservice.dto.response.ModeratorResponse;
import com.reveria.userservice.exception.UsernameAlreadyExistsException;
import com.reveria.userservice.mapper.ModeratorAuthMapper;
import com.reveria.userservice.model.entity.Moderator;
import com.reveria.userservice.repository.ModeratorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModeratorManagementService {

    private final ModeratorRepository moderatorRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModeratorAuthMapper moderatorAuthMapper;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public ModeratorResponse createModerator(CreateModeratorRequest request) {
        if (moderatorRepository.existsByUsername(request.getUsername().toLowerCase())) {
            throw new UsernameAlreadyExistsException(request.getUsername());
        }

        Moderator moderator = Moderator.builder()
                .username(request.getUsername().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getDisplayName() != null
                        ? request.getDisplayName()
                        : request.getUsername())
                .role(request.getRole())
                .active(true)
                .build();

        moderator = moderatorRepository.save(moderator);
        log.info("New moderator created: {} with role: {}", moderator.getUsername(), moderator.getRole());

        return moderatorAuthMapper.toModeratorResponse(moderator);
    }

    public List<ModeratorResponse> listModerators() {
        return moderatorRepository.findAll().stream()
                .map(moderatorAuthMapper::toModeratorResponse)
                .toList();
    }

    public ModeratorResponse getModerator(String uuid) {
        Moderator moderator = moderatorRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("Moderator not found"));

        return moderatorAuthMapper.toModeratorResponse(moderator);
    }

    @Transactional
    public ModeratorResponse deactivateModerator(String targetUuid, String callerUuid) {
        if (targetUuid.equals(callerUuid)) {
            throw new IllegalArgumentException("Cannot deactivate your own account");
        }

        Moderator moderator = moderatorRepository.findByUuid(targetUuid)
                .orElseThrow(() -> new IllegalArgumentException("Moderator not found"));

        if (!moderator.getActive()) {
            throw new IllegalStateException("Moderator is already deactivated");
        }

        moderator.setActive(false);
        moderatorRepository.save(moderator);

        refreshTokenService.revokeAllModeratorSessions(moderator.getId());

        log.info("Moderator deactivated: {}", moderator.getUsername());
        return moderatorAuthMapper.toModeratorResponse(moderator);
    }

    @Transactional
    public ModeratorResponse activateModerator(String uuid) {
        Moderator moderator = moderatorRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("Moderator not found"));

        if (moderator.getActive()) {
            throw new IllegalStateException("Moderator is already active");
        }

        moderator.setActive(true);
        moderatorRepository.save(moderator);

        log.info("Moderator activated: {}", moderator.getUsername());
        return moderatorAuthMapper.toModeratorResponse(moderator);
    }
}
