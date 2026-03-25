package com.reveria.contentservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChannelResponse {

    private String uuid;
    private String ownerUuid;
    private String name;
    private String description;
    private String bannerUrl;
    private String avatarUrl;
    private String status;
    private Long subscriberCount;
    private String streamKey;
    private String rtmpIngestUrl;
    private String createdAt;
}
