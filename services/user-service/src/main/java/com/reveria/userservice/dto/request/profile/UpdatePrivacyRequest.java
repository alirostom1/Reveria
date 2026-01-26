package com.reveria.userservice.dto.request.profile;

import com.reveria.userservice.model.enums.MessagePrivacy;
import com.reveria.userservice.model.enums.ProfileVisibility;
import lombok.Data;

@Data
public class UpdatePrivacyRequest {

    private ProfileVisibility profileVisibility;
    private Boolean showOnlineStatus;
    private Boolean allowDirectMessages;
    private Boolean allowFriendRequests;
    private MessagePrivacy messagePrivacy;
}