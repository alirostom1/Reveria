package com.reveria.contentservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ffmpeg")
@Data
public class FfmpegConfig {

    private String path = "ffmpeg";
    private String ffprobePath = "ffprobe";
    private String tempDir = "/tmp/reveria-transcoding";
}
