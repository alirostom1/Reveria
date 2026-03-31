package com.reveria.socialservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RelationshipStatusResponse {
    private String friendshipStatus; // null, PENDING, ACCEPTED, DECLINED
    private Long friendshipId;       // for accept/decline/remove actions
    private boolean friendRequestFromMe; // true if I sent the request
    private boolean following;
    private boolean followedBy;
    private boolean blocked;
    private boolean blockedBy;
}
