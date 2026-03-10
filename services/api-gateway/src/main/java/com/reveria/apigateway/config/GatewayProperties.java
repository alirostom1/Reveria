package com.reveria.apigateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {

    private Map<String, RouteConfig> routes;
    private List<String> publicPaths;

    @Data
    public static class RouteConfig {
        private String url;
        private List<String> prefixes;
    }
}

