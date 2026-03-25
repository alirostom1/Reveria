package com.reveria.contentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateLiveStreamRequest {

    @NotBlank
    private String channelUuid;

    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 2000)
    private String description;
}
