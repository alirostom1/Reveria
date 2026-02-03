package com.reveria.userservice.controller;

import com.reveria.userservice.dto.request.moderator.CreateModeratorRequest;
import com.reveria.userservice.dto.response.ApiResponse;
import com.reveria.userservice.dto.response.ModeratorResponse;
import com.reveria.userservice.security.ModeratorPrincipal;
import com.reveria.userservice.service.ModeratorManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mod/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class ModeratorManagementController {

    private final ModeratorManagementService moderatorManagementService;

    @PostMapping("/moderators")
    public ResponseEntity<ApiResponse<ModeratorResponse>> createModerator(
            @Valid @RequestBody CreateModeratorRequest request
    ) {
        ModeratorResponse data = moderatorManagementService.createModerator(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(data, "Moderator created successfully"));
    }

    @GetMapping("/moderators")
    public ResponseEntity<ApiResponse<List<ModeratorResponse>>> listModerators() {
        List<ModeratorResponse> data = moderatorManagementService.listModerators();
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/moderators/{uuid}")
    public ResponseEntity<ApiResponse<ModeratorResponse>> getModerator(
            @PathVariable String uuid
    ) {
        ModeratorResponse data = moderatorManagementService.getModerator(uuid);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("/moderators/{uuid}/deactivate")
    public ResponseEntity<ApiResponse<ModeratorResponse>> deactivateModerator(
            @PathVariable String uuid,
            @AuthenticationPrincipal ModeratorPrincipal principal
    ) {
        ModeratorResponse data = moderatorManagementService.deactivateModerator(
                uuid, principal.getModerator().getUuid()
        );
        return ResponseEntity.ok(ApiResponse.success(data, "Moderator deactivated"));
    }

    @PostMapping("/moderators/{uuid}/activate")
    public ResponseEntity<ApiResponse<ModeratorResponse>> activateModerator(
            @PathVariable String uuid
    ) {
        ModeratorResponse data = moderatorManagementService.activateModerator(uuid);
        return ResponseEntity.ok(ApiResponse.success(data, "Moderator activated"));
    }
}
