package com.reveria.contentservice.mapper;

import com.reveria.contentservice.config.LiveStreamConfig;
import com.reveria.contentservice.dto.response.ChannelResponse;
import com.reveria.contentservice.model.entity.Channel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class ChannelMapper {

    private final LiveStreamConfig liveStreamConfig;

    public ChannelResponse toResponse(Channel channel) {
        return baseBuilder(channel).build();
    }

    public ChannelResponse toOwnerResponse(Channel channel) {
        return baseBuilder(channel)
                .streamKey(channel.getStreamKey())
                .rtmpIngestUrl(buildRtmpIngestUrl())
                .build();
    }

    private ChannelResponse.ChannelResponseBuilder baseBuilder(Channel channel) {
        return ChannelResponse.builder()
                .uuid(channel.getUuid())
                .ownerUuid(channel.getOwnerUuid())
                .name(channel.getName())
                .description(channel.getDescription())
                .bannerUrl(channel.getBannerUrl())
                .avatarUrl(channel.getAvatarUrl())
                .status(channel.getStatus().name())
                .subscriberCount(channel.getSubscriberCount())
                .createdAt(formatDateTime(channel.getCreatedAt()));
    }

    private String buildRtmpIngestUrl() {
        return "rtmp://" + liveStreamConfig.getRtmpHost() + ":" + liveStreamConfig.getRtmpPort() + "/live";
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
