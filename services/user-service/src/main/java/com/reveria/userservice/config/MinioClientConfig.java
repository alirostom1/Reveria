package com.reveria.userservice.config;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MinioClientConfig {

    private final StorageConfig storageConfig;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(storageConfig.getEndpoint())
                .credentials(storageConfig.getAccessKey(), storageConfig.getSecretKey())
                .build();
    }
}
