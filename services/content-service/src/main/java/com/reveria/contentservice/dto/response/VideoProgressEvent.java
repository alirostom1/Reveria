package com.reveria.contentservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VideoProgressEvent {
    private String videoUuid;
    private String status;
    private String step;
    private int progress;
    private String message;
}
