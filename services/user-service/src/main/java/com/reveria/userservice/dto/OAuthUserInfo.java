package com.reveria.userservice.dto;

import com.reveria.userservice.model.enums.ProviderType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OAuthUserInfo {
    private ProviderType provider;
    private String providerId;
    private String email;
    private String name;
    private String pictureUrl;
}