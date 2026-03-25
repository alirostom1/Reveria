package com.reveria.contentservice.controller;

import com.reveria.contentservice.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.Instant;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat/{streamUuid}")
    public void sendMessage(@DestinationVariable String streamUuid, ChatMessage message) {
        message.setStreamUuid(streamUuid);
        message.setTimestamp(Instant.now().toString());
        messagingTemplate.convertAndSend("/topic/chat/" + streamUuid, message);
    }
}
