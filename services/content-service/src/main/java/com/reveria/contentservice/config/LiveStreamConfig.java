package com.reveria.contentservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "livestream")
@Data
public class LiveStreamConfig {

    private String rtmpHost;
    private int rtmpPort;
    private String hlsBaseUrl;
}
