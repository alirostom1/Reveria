package com.reveria.userservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "storage.minio")
@Data
public class StorageConfig {

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;
}
