package com.reveria.contentservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reveria.contentservice.config.FfmpegConfig;
import com.reveria.contentservice.exception.TranscodingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class FfmpegService {

    private final FfmpegConfig ffmpegConfig;
    private final ObjectMapper objectMapper;

    public record ProbeResult(int durationSeconds, int width, int height) {}

    public record HlsVariant(String name, int width, int height, String videoBitrate, String maxRate, String bufSize, String audioBitrate) {
        public String resolution() {
            return width + "x" + height;
        }
    }

    public ProbeResult probe(File inputFile) {
        try {
            List<String> command = List.of(
                    ffmpegConfig.getFfprobePath(),
                    "-v", "quiet",
                    "-print_format", "json",
                    "-show_format",
                    "-show_streams",
                    inputFile.getAbsolutePath()
            );

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String output = new String(process.getInputStream().readAllBytes());

            if (!process.waitFor(60, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new TranscodingException("ffprobe timed out");
            }

            if (process.exitValue() != 0) {
                throw new TranscodingException("ffprobe failed: " + output);
            }

            JsonNode root = objectMapper.readTree(output);

            // Get duration from format
            double duration = root.path("format").path("duration").asDouble(0);

            // Find video stream
            int width = 0, height = 0;
            for (JsonNode stream : root.path("streams")) {
                if ("video".equals(stream.path("codec_type").asText())) {
                    width = stream.path("width").asInt();
                    height = stream.path("height").asInt();
                    if (duration == 0) {
                        duration = stream.path("duration").asDouble(0);
                    }
                    break;
                }
            }

            return new ProbeResult((int) duration, width, height);
        } catch (TranscodingException e) {
            throw e;
        } catch (Exception e) {
            throw new TranscodingException("Failed to probe video", e);
        }
    }

    public File generateThumbnail(File inputFile, Path outputDir, int atSeconds) {
        File thumbnailFile = outputDir.resolve("thumbnail.jpg").toFile();

        List<String> command = List.of(
                ffmpegConfig.getPath(),
                "-i", inputFile.getAbsolutePath(),
                "-ss", String.valueOf(atSeconds),
                "-vframes", "1",
                "-vf", "scale=640:-2",
                "-q:v", "2",
                "-y",
                thumbnailFile.getAbsolutePath()
        );

        runProcess(command, "thumbnail generation", 30);
        return thumbnailFile;
    }

    public void transcodeToHls(File inputFile, Path outputDir, List<HlsVariant> variants) {
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            throw new TranscodingException("Failed to create output directory", e);
        }

        // Transcode each variant separately
        for (HlsVariant variant : variants) {
            log.info("Transcoding variant: {} ({})", variant.name(), variant.resolution());

            String segmentPattern = outputDir.resolve(variant.name() + "_%03d.ts").toString();
            String playlistPath = outputDir.resolve(variant.name() + ".m3u8").toString();

            List<String> command = new ArrayList<>(List.of(
                    ffmpegConfig.getPath(),
                    "-i", inputFile.getAbsolutePath(),
                    "-vf", "scale=" + variant.resolution(),
                    "-c:v", "libx264",
                    "-preset", "fast",
                    "-b:v", variant.videoBitrate(),
                    "-maxrate", variant.maxRate(),
                    "-bufsize", variant.bufSize(),
                    "-c:a", "aac",
                    "-b:a", variant.audioBitrate(),
                    "-ac", "2",
                    "-hls_time", "6",
                    "-hls_playlist_type", "vod",
                    "-hls_segment_filename", segmentPattern,
                    "-y",
                    playlistPath
            ));

            runProcess(command, "HLS transcode " + variant.name(), 1800); // 30 min timeout per variant
        }

        // Generate master playlist
        generateMasterPlaylist(outputDir, variants);
    }

    public void transcodeToMp4(File inputFile, Path outputDir, HlsVariant variant) {
        String mp4Path = outputDir.resolve(variant.name() + ".mp4").toString();

        List<String> command = new ArrayList<>(List.of(
                ffmpegConfig.getPath(),
                "-i", inputFile.getAbsolutePath(),
                "-vf", "scale=" + variant.resolution(),
                "-c:v", "libx264",
                "-preset", "fast",
                "-b:v", variant.videoBitrate(),
                "-maxrate", variant.maxRate(),
                "-bufsize", variant.bufSize(),
                "-c:a", "aac",
                "-b:a", variant.audioBitrate(),
                "-ac", "2",
                "-movflags", "+faststart",
                "-y",
                mp4Path
        ));

        runProcess(command, "MP4 transcode " + variant.name(), 1800);
    }

    public List<HlsVariant> determineVariants(int sourceHeight) {
        List<HlsVariant> variants = new ArrayList<>();

        // Always include 360p
        variants.add(new HlsVariant("360p", 640, 360, "800k", "856k", "1200k", "96k"));

        if (sourceHeight >= 480) {
            variants.add(new HlsVariant("480p", 854, 480, "1400k", "1498k", "2100k", "128k"));
        }
        if (sourceHeight >= 720) {
            variants.add(new HlsVariant("720p", 1280, 720, "2800k", "2996k", "4200k", "128k"));
        }
        if (sourceHeight >= 1080) {
            variants.add(new HlsVariant("1080p", 1920, 1080, "5000k", "5350k", "7500k", "192k"));
        }

        return variants;
    }

    private void generateMasterPlaylist(Path outputDir, List<HlsVariant> variants) {
        StringBuilder sb = new StringBuilder();
        sb.append("#EXTM3U\n");

        for (HlsVariant variant : variants) {
            int bandwidth = parseBitrate(variant.videoBitrate()) + parseBitrate(variant.audioBitrate());
            sb.append("#EXT-X-STREAM-INF:BANDWIDTH=").append(bandwidth)
                    .append(",RESOLUTION=").append(variant.resolution())
                    .append(",NAME=\"").append(variant.name()).append("\"\n");
            sb.append(variant.name()).append(".m3u8\n");
        }

        try {
            Files.writeString(outputDir.resolve("master.m3u8"), sb.toString());
        } catch (IOException e) {
            throw new TranscodingException("Failed to write master playlist", e);
        }
    }

    private int parseBitrate(String bitrate) {
        String number = bitrate.replaceAll("[^0-9]", "");
        return Integer.parseInt(number) * 1000;
    }

    private void runProcess(List<String> command, String description, int timeoutSeconds) {
        try {
            log.debug("Running {}: {}", description, String.join(" ", command));

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Drain output to prevent blocking
            String output = new String(process.getInputStream().readAllBytes());

            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new TranscodingException(description + " timed out after " + timeoutSeconds + "s");
            }

            if (process.exitValue() != 0) {
                log.error("{} failed with output: {}", description, output);
                throw new TranscodingException(description + " failed (exit code " + process.exitValue() + ")");
            }

            log.debug("{} completed successfully", description);
        } catch (TranscodingException e) {
            throw e;
        } catch (Exception e) {
            throw new TranscodingException("Failed to run " + description, e);
        }
    }
}
