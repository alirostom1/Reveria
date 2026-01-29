package com.reveria.userservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "security.rate-limit")
@Data
public class RateLimitConfig {

    // Account-based protection
    private int maxFailedAttempts = 5;
    private int lockoutDurationMinutes = 15;

    // IP-based protection
    private int maxFailedAttemptsPerIp = 20;
    private int ipBlockDurationMinutes = 30;

    // Time window for counting attempts (in minutes)
    private int attemptWindowMinutes = 60;

    // Cleanup (in days)
    private int attemptRetentionDays = 7;
}
