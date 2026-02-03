package com.reveria.userservice.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Email or username is required")
    private String identifier;

    @NotBlank(message = "Password is required")
    private String password;

    private boolean rememberMe;
}