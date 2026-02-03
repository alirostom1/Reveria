package com.reveria.userservice.config;

import lombok.Getter;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class KafkaConfig {

    @Value("${kafka.topic.user-events:user-events}")
    private String userEventsTopic;

    @Bean
    public NewTopic userEventsTopic() {
        return new NewTopic(userEventsTopic, 3, (short) 1);
    }
}
