package com.reveria.userservice.dto.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.reveria.userservice.model.enums.UserEventType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserEvent {
    private String eventId;
    private UserEventType eventType;
    private String userId;
    private Instant timestamp;
    private Map<String, Object> payload;
}
