package com.reveria.userservice.dto.request.profile;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeactivateAccountRequest {

    @NotBlank(message = "Password is required to deactivate account")
    private String password;

    private String reason;
}