package com.reveria.contentservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RenditionResponse {

    private String quality;
    private String hlsPlaylistUrl;
    private String mp4FileUrl;
    private Integer width;
    private Integer height;
    private Integer bitrate;
    private Long fileSizeBytes;
    private String status;
}
