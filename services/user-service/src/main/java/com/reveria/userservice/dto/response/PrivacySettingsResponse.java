package com.reveria.userservice.dto.response;

import com.reveria.userservice.model.enums.MessagePrivacy;
import com.reveria.userservice.model.enums.ProfileVisibility;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PrivacySettingsResponse {

    private ProfileVisibility profileVisibility;
    private boolean showOnlineStatus;
    private boolean allowDirectMessages;
    private boolean allowFriendRequests;
    private MessagePrivacy messagePrivacy;
}