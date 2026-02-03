package com.reveria.userservice.dto.request.moderator;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ModeratorLoginRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;
}
