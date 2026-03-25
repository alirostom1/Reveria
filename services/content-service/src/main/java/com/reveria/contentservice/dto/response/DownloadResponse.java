package com.reveria.contentservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadResponse {

    private String downloadToken;
    private String downloadUrl;
    private String quality;
    private Long fileSizeBytes;
    private LocalDateTime expiresAt;
}
