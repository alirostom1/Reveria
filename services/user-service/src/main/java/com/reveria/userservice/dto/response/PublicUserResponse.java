package com.reveria.userservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicUserResponse {

    private String uuid;
    private String username;
    private String displayName;
    private String avatarUrl;
    private String bio;
    private boolean allowFriendRequests;
    private String profileVisibility;
    private String createdAt;
}
