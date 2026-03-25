package com.reveria.contentservice.mapper;

import com.reveria.contentservice.dto.response.LiveStreamResponse;
import com.reveria.contentservice.model.entity.LiveStream;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class LiveStreamMapper {

    public LiveStreamResponse toResponse(LiveStream stream) {
        return LiveStreamResponse.builder()
                .uuid(stream.getUuid())
                .hostUuid(stream.getHostUuid())
                .channelUuid(stream.getChannel() != null ? stream.getChannel().getUuid() : null)
                .channelName(stream.getChannel() != null ? stream.getChannel().getName() : null)
                .title(stream.getTitle())
                .description(stream.getDescription())
                .hlsPlaybackUrl(stream.getHlsPlaybackUrl())
                .status(stream.getStatus().name())
                .viewerCount(stream.getViewerCount())
                .startedAt(formatDateTime(stream.getStartedAt()))
                .endedAt(formatDateTime(stream.getEndedAt()))
                .build();
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
