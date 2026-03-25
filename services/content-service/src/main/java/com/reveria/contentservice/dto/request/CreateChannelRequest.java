package com.reveria.contentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateChannelRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 2000)
    private String description;
}
