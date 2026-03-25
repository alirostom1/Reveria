package com.reveria.contentservice.service;

import com.reveria.contentservice.config.FfmpegConfig;
import com.reveria.contentservice.dto.response.VideoResponse;
import com.reveria.contentservice.exception.ResourceNotFoundException;
import com.reveria.contentservice.exception.VideoValidationException;
import com.reveria.contentservice.mapper.VideoMapper;
import com.reveria.contentservice.model.entity.Channel;
import com.reveria.contentservice.model.entity.Video;
import com.reveria.contentservice.model.entity.VideoCategory;
import com.reveria.contentservice.model.entity.VideoRendition;
import com.reveria.contentservice.model.entity.VideoTag;
import com.reveria.contentservice.model.enums.VideoStatus;
import com.reveria.contentservice.model.enums.VideoVisibility;
import com.reveria.contentservice.repository.VideoCategoryRepository;
import com.reveria.contentservice.repository.VideoRenditionRepository;
import com.reveria.contentservice.repository.VideoRepository;
import com.reveria.contentservice.repository.VideoTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoService {

    private final VideoRepository videoRepository;
    private final VideoRenditionRepository renditionRepository;
    private final VideoTagRepository tagRepository;
    private final VideoCategoryRepository categoryRepository;
    private final VideoMapper videoMapper;
    private final TranscodingService transcodingService;
    private final ChannelService channelService;
    private final FfmpegConfig ffmpegConfig;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "video/mp4",
            "video/quicktime",
            "video/x-msvideo",
            "video/x-matroska",
            "video/webm"
    );

    @Transactional
    public VideoResponse uploadVideo(String userUuid, String channelUuid, String title,
                                     String description, String visibility, String categorySlug,
                                     List<String> tagNames, MultipartFile file) {
        validateVideoFile(file);

        Channel channel = channelService.getChannelEntity(channelUuid);
        if (!channel.getOwnerUuid().equals(userUuid)) {
            throw new IllegalStateException("Not the channel owner");
        }

        VideoVisibility vis = visibility != null
                ? VideoVisibility.valueOf(visibility.toUpperCase())
                : VideoVisibility.PUBLIC;

        Set<VideoTag> tags = resolveTags(tagNames);

        VideoCategory category = null;
        if (categorySlug != null && !categorySlug.isBlank()) {
            category = categoryRepository.findBySlug(categorySlug).orElse(null);
        }

        Video video = Video.builder()
                .userUuid(userUuid)
                .channel(channel)
                .title(title)
                .description(description)
                .visibility(vis)
                .category(category)
                .originalFileKey(file.getOriginalFilename())
                .tags(tags)
                .build();
        video = videoRepository.save(video);

        File rawFile = saveToTempFile(file, video.getUuid());

        Long videoId = video.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                transcodingService.transcodeVideo(videoId, rawFile);
            }
        });

        log.info("Video {} upload accepted, transcoding queued", video.getUuid());
        return videoMapper.toResponse(video);
    }

    public Page<VideoResponse> listVideos(int page, int size, String category, String tag, String search, String sort) {
        Sort sorting = switch (sort) {
            case "popular" -> Sort.by(Sort.Direction.DESC, "viewCount");
            case "most-liked" -> Sort.by(Sort.Direction.DESC, "likeCount");
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "createdAt"); // "latest"
        };

        Specification<Video> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), VideoStatus.READY));
            predicates.add(cb.equal(root.get("visibility"), VideoVisibility.PUBLIC));

            if (category != null && !category.isBlank()) {
                Join<Video, VideoCategory> catJoin = root.join("category", JoinType.INNER);
                predicates.add(cb.equal(catJoin.get("slug"), category));
            }

            if (tag != null && !tag.isBlank()) {
                Join<Video, VideoTag> tagJoin = root.join("tags", JoinType.INNER);
                predicates.add(cb.equal(tagJoin.get("slug"), tag));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }

            query.distinct(true);
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return videoRepository.findAll(spec, PageRequest.of(page, size, sorting))
                .map(videoMapper::toResponse);
    }

    public Page<VideoResponse> listUserVideos(String userUuid, Pageable pageable) {
        return videoRepository.findByUserUuidOrderByCreatedAtDesc(userUuid, pageable)
                .map(videoMapper::toResponse);
    }

    public Page<VideoResponse> listChannelVideos(String channelUuid, Pageable pageable) {
        Channel channel = channelService.getChannelEntity(channelUuid);
        return videoRepository
                .findByChannelIdAndStatusOrderByCreatedAtDesc(channel.getId(), VideoStatus.READY, pageable)
                .map(videoMapper::toResponse);
    }

    public VideoResponse getVideo(String uuid) {
        Video video = videoRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Video", uuid));
        List<VideoRendition> renditions = renditionRepository.findByVideoIdOrderByQuality(video.getId());
        return videoMapper.toResponse(video, renditions);
    }

    private Set<VideoTag> resolveTags(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) return new HashSet<>();

        Set<VideoTag> tags = new HashSet<>();
        for (String name : tagNames) {
            String trimmed = name.trim();
            if (trimmed.isEmpty()) continue;

            VideoTag tag = tagRepository.findByNameIgnoreCase(trimmed)
                    .orElseGet(() -> tagRepository.save(VideoTag.builder()
                            .name(trimmed)
                            .slug(trimmed.toLowerCase().replaceAll("[^a-z0-9]+", "-"))
                            .build()));
            tag.setUsageCount(tag.getUsageCount() + 1);
            tags.add(tag);
        }
        return tags;
    }

    private void validateVideoFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new VideoValidationException("file", "Video file is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new VideoValidationException("file",
                    "Unsupported video format. Allowed: MP4, MOV, AVI, MKV, WebM");
        }
    }

    private File saveToTempFile(MultipartFile file, String videoUuid) {
        try {
            Path tempDir = Path.of(ffmpegConfig.getTempDir());
            Files.createDirectories(tempDir);
            String extension = getExtension(file.getOriginalFilename());
            File tempFile = tempDir.resolve(videoUuid + extension).toFile();
            file.transferTo(tempFile);
            return tempFile;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save uploaded file", e);
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return ".mp4";
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex >= 0 ? filename.substring(dotIndex) : ".mp4";
    }
}
