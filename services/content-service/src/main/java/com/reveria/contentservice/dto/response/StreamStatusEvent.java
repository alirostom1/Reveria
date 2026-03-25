package com.reveria.contentservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StreamStatusEvent {
    private String streamUuid;
    private String channelUuid;
    private String status;
    private String hlsPlaybackUrl;
}
