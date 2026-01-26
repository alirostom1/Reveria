package com.reveria.userservice.dto.response;

import com.reveria.userservice.model.enums.ProviderType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LinkedProviderResponse {

    private ProviderType provider;
    private String linkedAt;
    private boolean canUnlink;
}