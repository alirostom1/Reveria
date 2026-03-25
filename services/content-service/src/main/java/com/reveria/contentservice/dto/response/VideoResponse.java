package com.reveria.contentservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class VideoResponse {

    private String uuid;
    private String title;
    private String description;
    private String status;
    private String visibility;
    private String hlsManifestUrl;
    private String thumbnailUrl;
    private Long durationSeconds;
    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    private boolean allowComments;
    private boolean allowDownloads;
    private String userUuid;
    private String channelUuid;
    private String channelName;
    private String categoryName;
    private List<String> tags;
    private List<RenditionResponse> renditions;
    private String publishedAt;
    private String createdAt;
}
