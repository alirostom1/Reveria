package com.reveria.contentservice.mapper;

import com.reveria.contentservice.dto.response.RenditionResponse;
import com.reveria.contentservice.dto.response.VideoResponse;
import com.reveria.contentservice.model.entity.Video;
import com.reveria.contentservice.model.entity.VideoRendition;
import com.reveria.contentservice.model.entity.VideoTag;
import com.reveria.contentservice.model.enums.VideoStatus;
import com.reveria.contentservice.service.MinioStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class VideoMapper {

    private final MinioStorageService storageService;

    public VideoResponse toResponse(Video video) {
        return toResponse(video, Collections.emptyList());
    }

    public VideoResponse toResponse(Video video, List<VideoRendition> renditions) {
        return VideoResponse.builder()
                .uuid(video.getUuid())
                .title(video.getTitle())
                .description(video.getDescription())
                .status(video.getStatus().name())
                .visibility(video.getVisibility().name())
                .hlsManifestUrl(video.getStatus() == VideoStatus.READY
                        ? storageService.buildUrl("videos/" + video.getUuid() + "/hls/master.m3u8") : null)
                .thumbnailUrl(video.getThumbnailUrl())
                .durationSeconds(video.getDurationSeconds())
                .viewCount(video.getViewCount())
                .likeCount(video.getLikeCount())
                .commentCount(video.getCommentCount())
                .allowComments(video.isAllowComments())
                .allowDownloads(video.isAllowDownloads())
                .userUuid(video.getUserUuid())
                .channelUuid(video.getChannel() != null ? video.getChannel().getUuid() : null)
                .channelName(video.getChannel() != null ? video.getChannel().getName() : null)
                .categoryName(video.getCategory() != null ? video.getCategory().getName() : null)
                .tags(video.getTags() != null
                        ? video.getTags().stream().map(VideoTag::getName).toList()
                        : Collections.emptyList())
                .renditions(renditions.stream().map(this::toRenditionResponse).toList())
                .publishedAt(formatDateTime(video.getPublishedAt()))
                .createdAt(formatDateTime(video.getCreatedAt()))
                .build();
    }

    public RenditionResponse toRenditionResponse(VideoRendition rendition) {
        return RenditionResponse.builder()
                .quality(rendition.getQuality().name())
                .hlsPlaylistUrl(rendition.getHlsPlaylistKey() != null
                        ? storageService.buildUrl(rendition.getHlsPlaylistKey()) : null)
                .mp4FileUrl(rendition.getMp4FileKey() != null
                        ? storageService.buildUrl(rendition.getMp4FileKey()) : null)
                .width(rendition.getWidth())
                .height(rendition.getHeight())
                .bitrate(rendition.getBitrate())
                .fileSizeBytes(rendition.getFileSizeBytes())
                .status(rendition.getStatus().name())
                .build();
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
