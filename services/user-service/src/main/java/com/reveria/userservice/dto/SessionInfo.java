package com.reveria.userservice.dto;

public record SessionInfo(
        String ipAddress,
        String userAgent
) {
}
