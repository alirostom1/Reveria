package com.reveria.contentservice.service;

import com.reveria.contentservice.config.FfmpegConfig;
import com.reveria.contentservice.dto.response.VideoProgressEvent;
import com.reveria.contentservice.model.entity.Video;
import com.reveria.contentservice.model.entity.VideoRendition;
import com.reveria.contentservice.model.enums.RenditionStatus;
import com.reveria.contentservice.model.enums.VideoQuality;
import com.reveria.contentservice.model.enums.VideoStatus;
import com.reveria.contentservice.repository.VideoRenditionRepository;
import com.reveria.contentservice.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranscodingService {

    private final VideoRepository videoRepository;
    private final VideoRenditionRepository renditionRepository;
    private final MinioStorageService storageService;
    private final FfmpegService ffmpegService;
    private final FfmpegConfig ffmpegConfig;
    private final SimpMessagingTemplate messagingTemplate;

    @Async("transcodingExecutor")
    @Transactional
    public void transcodeVideo(Long videoId, File rawFile) {
        Video video = videoRepository.findById(videoId).orElse(null);
        if (video == null) {
            log.error("Video not found for transcoding: {}", videoId);
            cleanup(rawFile, null);
            return;
        }

        video.setStatus(VideoStatus.PROCESSING);
        videoRepository.save(video);
        broadcastProgress(video.getUuid(), "PROCESSING", "PROBING", 5, "Analyzing video...");

        Path outputDir = Path.of(ffmpegConfig.getTempDir(), video.getUuid());

        try {
            // 1. Probe video
            FfmpegService.ProbeResult probe = ffmpegService.probe(rawFile);
            video.setDurationSeconds((long) probe.durationSeconds());
            log.info("Video {} probed: {}x{}, {}s", video.getUuid(), probe.width(), probe.height(), probe.durationSeconds());

            // 2. Generate thumbnail
            broadcastProgress(video.getUuid(), "PROCESSING", "THUMBNAIL", 10, "Generating thumbnail...");
            Files.createDirectories(outputDir);
            int thumbnailAt = Math.min(1, probe.durationSeconds() / 4);
            File thumbnail = ffmpegService.generateThumbnail(rawFile, outputDir, thumbnailAt);

            // 3. Determine quality variants
            List<FfmpegService.HlsVariant> variants = ffmpegService.determineVariants(probe.height());
            log.info("Video {} will be transcoded to: {}", video.getUuid(),
                    variants.stream().map(FfmpegService.HlsVariant::name).toList());

            // 4. Create rendition records as PENDING
            for (FfmpegService.HlsVariant variant : variants) {
                VideoRendition rendition = VideoRendition.builder()
                        .video(video)
                        .quality(mapQuality(variant.name()))
                        .width(variant.width())
                        .height(variant.height())
                        .bitrate(parseBitrate(variant.videoBitrate()))
                        .status(RenditionStatus.PENDING)
                        .build();
                renditionRepository.save(rendition);
            }

            // 5. Transcode to HLS
            int totalVariants = variants.size();
            for (int i = 0; i < totalVariants; i++) {
                int progressBase = 15 + (i * 50 / totalVariants);
                int progressEnd = 15 + ((i + 1) * 50 / totalVariants);
                String variantName = variants.get(i).name();
                broadcastProgress(video.getUuid(), "PROCESSING", "TRANSCODING",
                        progressBase, "Transcoding " + variantName + "...");
            }
            Path hlsDir = outputDir.resolve("hls");
            broadcastProgress(video.getUuid(), "PROCESSING", "TRANSCODING", 15, "Transcoding " + variants.get(0).name() + "...");
            ffmpegService.transcodeToHls(rawFile, hlsDir, variants);
            broadcastProgress(video.getUuid(), "PROCESSING", "TRANSCODING", 65, "Transcoding complete");

            // 6. Upload thumbnail to MinIO
            broadcastProgress(video.getUuid(), "PROCESSING", "UPLOADING", 70, "Uploading files...");
            String thumbPath = "videos/" + video.getUuid() + "/thumbnail.jpg";
            String thumbUrl = storageService.uploadFile(thumbPath, thumbnail, "image/jpeg");
            video.setThumbnailUrl(thumbUrl);

            // 7. Upload all HLS files to MinIO
            String hlsBasePath = "videos/" + video.getUuid() + "/hls/";
            uploadDirectory(hlsDir, hlsBasePath);
            broadcastProgress(video.getUuid(), "PROCESSING", "UPLOADING", 78, "Uploading stream files...");

            // 8. Transcode to MP4 for downloads
            Path mp4Dir = outputDir.resolve("mp4");
            Files.createDirectories(mp4Dir);
            for (int i = 0; i < totalVariants; i++) {
                FfmpegService.HlsVariant variant = variants.get(i);
                int mp4Progress = 80 + (i * 15 / totalVariants);
                broadcastProgress(video.getUuid(), "PROCESSING", "MP4", mp4Progress,
                        "Creating download for " + variant.name() + "...");
                try {
                    log.info("Generating MP4 for variant: {}", variant.name());
                    ffmpegService.transcodeToMp4(rawFile, mp4Dir, variant);
                } catch (Exception e) {
                    log.warn("MP4 generation failed for variant {}: {}", variant.name(), e.getMessage());
                }
            }

            // 9. Upload MP4 files to MinIO
            broadcastProgress(video.getUuid(), "PROCESSING", "UPLOADING", 95, "Finishing up...");
            String mp4BasePath = "videos/" + video.getUuid() + "/mp4/";
            for (FfmpegService.HlsVariant variant : variants) {
                Path mp4File = mp4Dir.resolve(variant.name() + ".mp4");
                if (Files.exists(mp4File)) {
                    storageService.uploadFile(mp4BasePath + variant.name() + ".mp4", mp4File.toFile(), "video/mp4");
                }
            }

            // 10. Update rendition records with keys and mark as READY
            for (FfmpegService.HlsVariant variant : variants) {
                VideoQuality quality = mapQuality(variant.name());
                renditionRepository.findByVideoIdAndQuality(video.getId(), quality)
                        .ifPresent(rendition -> {
                            String playlistKey = hlsBasePath + variant.name() + ".m3u8";
                            rendition.setHlsPlaylistKey(playlistKey);
                            rendition.setStatus(RenditionStatus.READY);

                            Path mp4File = mp4Dir.resolve(variant.name() + ".mp4");
                            if (Files.exists(mp4File)) {
                                rendition.setMp4FileKey(mp4BasePath + variant.name() + ".mp4");
                                try {
                                    rendition.setFileSizeBytes(Files.size(mp4File));
                                } catch (IOException ignored) {}
                            } else {
                                try {
                                    long totalSize = Files.list(hlsDir)
                                            .filter(p -> p.getFileName().toString().startsWith(variant.name()))
                                            .mapToLong(p -> {
                                                try { return Files.size(p); } catch (IOException e) { return 0; }
                                            })
                                            .sum();
                                    rendition.setFileSizeBytes(totalSize);
                                } catch (IOException ignored) {}
                            }

                            renditionRepository.save(rendition);
                        });
            }

            // 11. Mark video as ready
            video.setStatus(VideoStatus.READY);
            video.setPublishedAt(LocalDateTime.now());
            videoRepository.save(video);
            broadcastProgress(video.getUuid(), "READY", "READY", 100, "Video is ready!");

            log.info("Video {} transcoded successfully", video.getUuid());

        } catch (Exception e) {
            log.error("Transcoding failed for video {}: {}", video.getUuid(), e.getMessage(), e);
            video.setStatus(VideoStatus.FAILED);
            videoRepository.save(video);

            renditionRepository.findByVideoIdOrderByQuality(video.getId())
                    .forEach(r -> {
                        r.setStatus(RenditionStatus.FAILED);
                        renditionRepository.save(r);
                    });

            broadcastProgress(video.getUuid(), "FAILED", "FAILED", 0, "Processing failed");
        } finally {
            cleanup(rawFile, outputDir);
        }
    }

    private void broadcastProgress(String videoUuid, String status, String step, int progress, String message) {
        try {
            VideoProgressEvent event = VideoProgressEvent.builder()
                    .videoUuid(videoUuid)
                    .status(status)
                    .step(step)
                    .progress(progress)
                    .message(message)
                    .build();
            messagingTemplate.convertAndSend("/topic/videos/" + videoUuid, event);
        } catch (Exception e) {
            log.debug("Failed to broadcast progress for video {}: {}", videoUuid, e.getMessage());
        }
    }

    private VideoQuality mapQuality(String variantName) {
        return switch (variantName) {
            case "360p" -> VideoQuality.Q_360P;
            case "480p" -> VideoQuality.Q_480P;
            case "720p" -> VideoQuality.Q_720P;
            case "1080p" -> VideoQuality.Q_1080P;
            default -> VideoQuality.Q_360P;
        };
    }

    private int parseBitrate(String bitrate) {
        String number = bitrate.replaceAll("[^0-9]", "");
        return Integer.parseInt(number) * 1000;
    }

    private void uploadDirectory(Path directory, String basePath) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String relativePath = directory.relativize(file).toString();
                String minioPath = basePath + relativePath;
                String contentType = relativePath.endsWith(".m3u8")
                        ? "application/vnd.apple.mpegurl"
                        : "video/mp2t";
                storageService.uploadFile(minioPath, file.toFile(), contentType);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void cleanup(File rawFile, Path outputDir) {
        try {
            if (rawFile != null && rawFile.exists()) {
                rawFile.delete();
            }
            if (outputDir != null && Files.exists(outputDir)) {
                Files.walk(outputDir)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try { Files.deleteIfExists(path); } catch (IOException ignored) {}
                        });
            }
        } catch (Exception e) {
            log.warn("Failed to clean up temp files: {}", e.getMessage());
        }
    }
}
