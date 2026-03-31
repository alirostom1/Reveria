package com.reveria.socialservice.dto.response;

import com.reveria.socialservice.model.enums.FriendshipStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FriendshipResponse {
    private Long id;
    private String userUuid;
    private String friendUuid;
    private FriendshipStatus status;
    private LocalDateTime requestedAt;
    private LocalDateTime acceptedAt;
}
