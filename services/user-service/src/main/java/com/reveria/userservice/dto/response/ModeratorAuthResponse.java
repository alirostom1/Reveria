package com.reveria.userservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.reveria.userservice.model.enums.ModeratorRole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModeratorAuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private ModeratorInfo moderator;

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ModeratorInfo {
        private String uuid;
        private String username;
        private String displayName;
        private ModeratorRole role;
    }
}
