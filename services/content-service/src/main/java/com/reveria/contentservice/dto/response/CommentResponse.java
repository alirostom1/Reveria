package com.reveria.contentservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CommentResponse {

    private Long id;
    private String userUuid;
    private String content;
    private String createdAt;
}
