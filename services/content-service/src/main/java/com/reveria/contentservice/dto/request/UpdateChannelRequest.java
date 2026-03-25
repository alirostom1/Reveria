package com.reveria.contentservice.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateChannelRequest {

    @Size(max = 100)
    private String name;

    @Size(max = 2000)
    private String description;

    private String bannerUrl;

    private String avatarUrl;
}
