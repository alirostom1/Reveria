package com.reveria.socialservice.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentResponse {
    private Long id;
    private String userUuid;
    private String content;
    private List<ReplyResponse> replies;
    private LocalDateTime createdAt;
}
