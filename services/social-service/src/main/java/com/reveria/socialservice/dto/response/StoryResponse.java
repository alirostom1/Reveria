package com.reveria.socialservice.dto.response;

import com.reveria.socialservice.model.enums.MediaType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StoryResponse {
    private String uuid;
    private String authorUuid;
    private MediaType mediaType;
    private String url;
    private Integer durationSeconds;
    private Long viewCount;
    private boolean viewedByMe;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
