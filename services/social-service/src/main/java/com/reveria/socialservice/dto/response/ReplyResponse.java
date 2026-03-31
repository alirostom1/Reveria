package com.reveria.socialservice.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReplyResponse {
    private Long id;
    private String userUuid;
    private String content;
    private LocalDateTime createdAt;
}
