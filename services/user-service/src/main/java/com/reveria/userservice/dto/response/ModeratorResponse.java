package com.reveria.userservice.dto.response;

import com.reveria.userservice.model.enums.ModeratorRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ModeratorResponse {
    private String uuid;
    private String username;
    private String displayName;
    private ModeratorRole role;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
