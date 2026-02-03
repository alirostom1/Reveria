package com.reveria.userservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserProfileResponse {

    private String uuid;
    private String email;
    private String username;
    private String displayName;
    private String avatarUrl;
    private String bio;
    private boolean emailVerified;
    private String createdAt;
    private List<String> linkedProviders;
}