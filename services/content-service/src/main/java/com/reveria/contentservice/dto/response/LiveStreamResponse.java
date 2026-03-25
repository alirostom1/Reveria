package com.reveria.contentservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LiveStreamResponse {

    private String uuid;
    private String hostUuid;
    private String channelUuid;
    private String channelName;
    private String title;
    private String description;
    private String hlsPlaybackUrl;
    private String status;
    private Long viewerCount;
    private String startedAt;
    private String endedAt;
}
