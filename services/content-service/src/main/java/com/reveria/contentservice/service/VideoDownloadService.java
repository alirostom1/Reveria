package com.reveria.contentservice.service;

import com.reveria.contentservice.dto.response.DownloadResponse;
import com.reveria.contentservice.exception.ResourceNotFoundException;
import com.reveria.contentservice.exception.VideoValidationException;
import com.reveria.contentservice.model.entity.Video;
import com.reveria.contentservice.model.entity.VideoDownload;
import com.reveria.contentservice.model.entity.VideoRendition;
import com.reveria.contentservice.model.enums.RenditionStatus;
import com.reveria.contentservice.model.enums.VideoQuality;
import com.reveria.contentservice.repository.VideoDownloadRepository;
import com.reveria.contentservice.repository.VideoRenditionRepository;
import com.reveria.contentservice.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoDownloadService {

    private final VideoRepository videoRepository;
    private final VideoRenditionRepository renditionRepository;
    private final VideoDownloadRepository downloadRepository;
    private final MinioStorageService minioStorageService;

    @Transactional
    public DownloadResponse generateDownloadToken(String videoUuid, String userUuid, VideoQuality quality) {
        Video video = videoRepository.findByUuid(videoUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Video", videoUuid));

        if (!video.isAllowDownloads()) {
            throw new VideoValidationException("download", "Downloads are not allowed for this video");
        }

        VideoRendition rendition = renditionRepository.findByVideoIdAndQuality(video.getId(), quality)
                .filter(r -> r.getStatus() == RenditionStatus.READY)
                .orElseThrow(() -> new ResourceNotFoundException("Rendition",
                        "quality " + quality.name() + " for video " + videoUuid));

        if (rendition.getMp4FileKey() == null || rendition.getMp4FileKey().isBlank()) {
            throw new VideoValidationException("download", "MP4 file is not available for this rendition");
        }

        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);

        VideoDownload download = VideoDownload.builder()
                .videoId(video.getId())
                .userUuid(userUuid)
                .quality(quality)
                .expiresAt(expiresAt)
                .build();
        download = downloadRepository.save(download);

        String downloadUrl = "/api/content/videos/download/" + download.getDownloadToken();

        log.info("Generated download token for video {} quality {} by user {}", videoUuid, quality, userUuid);

        return DownloadResponse.builder()
                .downloadToken(download.getDownloadToken())
                .downloadUrl(downloadUrl)
                .quality(quality.name())
                .fileSizeBytes(rendition.getFileSizeBytes())
                .expiresAt(expiresAt)
                .build();
    }

    public record DownloadFile(String mp4FileKey, String filename, long fileSizeBytes) {}

    @Transactional
    public DownloadFile resolveDownload(String downloadToken) {
        VideoDownload download = downloadRepository.findByDownloadToken(downloadToken)
                .orElseThrow(() -> new ResourceNotFoundException("Download", downloadToken));

        if (download.getExpiresAt() != null && download.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new VideoValidationException("download", "Download link has expired");
        }

        download.setDownloadedAt(LocalDateTime.now());
        downloadRepository.save(download);

        Video video = videoRepository.findById(download.getVideoId())
                .orElseThrow(() -> new ResourceNotFoundException("Video", download.getVideoId().toString()));

        VideoRendition rendition = renditionRepository.findByVideoIdAndQuality(
                        download.getVideoId(), download.getQuality())
                .orElseThrow(() -> new ResourceNotFoundException("Rendition",
                        "quality " + download.getQuality().name() + " for video ID " + download.getVideoId()));

        String safeName = video.getTitle().replaceAll("[^a-zA-Z0-9._\\- ]", "").trim();
        if (safeName.isBlank()) safeName = "video";
        String filename = safeName + "_" + download.getQuality().name() + ".mp4";

        return new DownloadFile(rendition.getMp4FileKey(), filename, rendition.getFileSizeBytes() != null ? rendition.getFileSizeBytes() : 0);
    }

    public java.io.InputStream getFileStream(String mp4FileKey) {
        return minioStorageService.getObject(mp4FileKey);
    }

    /**
     * Check if the user has a Pro subscription.
     * TODO: Integrate with user-service to verify subscription status
     */
    public boolean isProUser(String userUuid) {
        return true;
    }
}
