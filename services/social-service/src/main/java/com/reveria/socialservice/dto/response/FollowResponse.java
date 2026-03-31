package com.reveria.socialservice.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FollowResponse {
    private Long id;
    private String followerUuid;
    private String followingUuid;
    private LocalDateTime createdAt;
}
