package com.reveria.contentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private String streamUuid;
    private String userUuid;
    private String displayName;
    private String avatarUrl;
    private String content;
    private String timestamp;
}
