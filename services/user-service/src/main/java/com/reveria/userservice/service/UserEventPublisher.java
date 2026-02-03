package com.reveria.userservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reveria.userservice.config.KafkaConfig;
import com.reveria.userservice.dto.event.UserEvent;
import com.reveria.userservice.model.enums.UserEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaConfig kafkaConfig;

    public void publish(UserEventType eventType, String userId, Map<String, Object> payload) {
        UserEvent event = UserEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .userId(userId)
                .timestamp(Instant.now())
                .payload(payload)
                .build();

        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(kafkaConfig.getUserEventsTopic(), userId, json)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish {} event for user {}: {}",
                                    eventType, userId, ex.getMessage());
                        } else {
                            log.debug("Published {} event for user {}", eventType, userId);
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize {} event for user {}: {}",
                    eventType, userId, e.getMessage());
        }
    }
}
