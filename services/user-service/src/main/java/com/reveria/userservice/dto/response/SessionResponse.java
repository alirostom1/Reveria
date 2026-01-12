package com.reveria.userservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SessionResponse {

    private String familyId;
    private String ipAddress;
    private String userAgent;
    private String createdAt;
    private boolean currentSession;
}