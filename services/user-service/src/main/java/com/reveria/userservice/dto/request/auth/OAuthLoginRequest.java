package com.reveria.userservice.dto.request.auth;

import com.reveria.userservice.model.enums.ProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OAuthLoginRequest {

    @NotNull(message = "Provider is required")
    private ProviderType provider;

    @NotBlank(message = "Authorization code is required")
    private String code;

    @NotBlank(message = "Redirect URI is required")
    private String redirectUri;
}