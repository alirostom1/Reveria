package com.reveria.userservice.controller;

import com.reveria.userservice.dto.response.ApiResponse;
import com.reveria.userservice.dto.response.PublicUserResponse;
import com.reveria.userservice.model.entity.User;
import com.reveria.userservice.model.entity.UserPrivacySettings;
import com.reveria.userservice.model.enums.ProfileVisibility;
import com.reveria.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<PublicUserResponse>>> searchUsers(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (q.trim().length() < 2) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Search query must be at least 2 characters"));
        }

        Page<PublicUserResponse> results = userRepository
                .searchByUsernameOrDisplayName(q.trim(), PageRequest.of(page, size, Sort.by("username")))
                .map(this::toPublicResponse);

        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<PublicUserResponse>> getPublicProfile(@PathVariable String uuid) {
        User user = userRepository.findByUuid(uuid)
                .orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        UserPrivacySettings privacy = user.getPrivacySettings();
        if (privacy != null && privacy.getProfileVisibility() == ProfileVisibility.PRIVATE) {
            return ResponseEntity.ok(ApiResponse.success(
                    PublicUserResponse.builder()
                            .uuid(user.getUuid())
                            .username(user.getUsername())
                            .displayName(user.getDisplayName())
                            .avatarUrl(user.getAvatarUrl())
                            .profileVisibility("PRIVATE")
                            .build()
            ));
        }

        return ResponseEntity.ok(ApiResponse.success(toPublicResponse(user)));
    }

    private PublicUserResponse toPublicResponse(User user) {
        UserPrivacySettings privacy = user.getPrivacySettings();
        return PublicUserResponse.builder()
                .uuid(user.getUuid())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .allowFriendRequests(privacy == null || privacy.getAllowFriendRequests())
                .profileVisibility(privacy != null ? privacy.getProfileVisibility().name() : "PUBLIC")
                .createdAt(user.getCreatedAt() != null
                        ? user.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .build();
    }
}
