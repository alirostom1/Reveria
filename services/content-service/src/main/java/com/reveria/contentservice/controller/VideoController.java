package com.reveria.contentservice.controller;

import com.reveria.contentservice.dto.request.CreateCommentRequest;
import com.reveria.contentservice.dto.response.ApiResponse;
import com.reveria.contentservice.dto.response.CommentResponse;
import com.reveria.contentservice.dto.response.DownloadResponse;
import com.reveria.contentservice.dto.response.VideoResponse;
import com.reveria.contentservice.model.enums.VideoQuality;
import com.reveria.contentservice.service.VideoDownloadService;
import com.reveria.contentservice.service.VideoInteractionService;
import com.reveria.contentservice.service.VideoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/api/content/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;
    private final VideoInteractionService interactionService;
    private final VideoDownloadService downloadService;

    // --- Video CRUD ---

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VideoResponse>> uploadVideo(
            @RequestHeader(value = "X-User-UUID", required = false) String userUuid,
            @RequestParam("channelUuid") String channelUuid,
            @RequestParam("title") @Size(max = 200) String title,
            @RequestParam(value = "description", required = false) @Size(max = 2000) String description,
            @RequestParam(value = "visibility", required = false) String visibility,
            @RequestParam(value = "categorySlug", required = false) String categorySlug,
            @RequestParam(value = "tags", required = false) List<String> tags,
            @RequestParam("file") MultipartFile file
    ) {
        if (userUuid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }
        VideoResponse response = videoService.uploadVideo(userUuid, channelUuid, title, description, visibility, categorySlug, tags, file);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(response, "Video upload started"));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<VideoResponse>> getVideo(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponse.success(videoService.getVideo(uuid)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<VideoResponse>>> listVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                videoService.listVideos(page, size, category, tag, search, sort)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Page<VideoResponse>>> listMyVideos(
            @RequestHeader(value = "X-User-UUID", required = false) String userUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        if (userUuid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }
        return ResponseEntity.ok(ApiResponse.success(
                videoService.listUserVideos(userUuid, PageRequest.of(page, size))));
    }

    @GetMapping("/channel/{channelUuid}")
    public ResponseEntity<ApiResponse<Page<VideoResponse>>> listChannelVideos(
            @PathVariable String channelUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                videoService.listChannelVideos(channelUuid, PageRequest.of(page, size))));
    }

    // --- Comments ---

    @PostMapping("/{uuid}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @PathVariable String uuid,
            @RequestHeader("X-User-UUID") String userUuid,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        CommentResponse response = interactionService.addComment(uuid, userUuid, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @GetMapping("/{uuid}/comments")
    public ResponseEntity<ApiResponse<Page<CommentResponse>>> getComments(
            @PathVariable String uuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                interactionService.getComments(uuid, PageRequest.of(page, size))));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long commentId,
            @RequestHeader("X-User-UUID") String userUuid
    ) {
        interactionService.deleteComment(commentId, userUuid);
        return ResponseEntity.ok(ApiResponse.success("Comment deleted"));
    }

    // --- Replies ---

    @PostMapping("/comments/{commentId}/replies")
    public ResponseEntity<ApiResponse<CommentResponse>> addReply(
            @PathVariable Long commentId,
            @RequestHeader("X-User-UUID") String userUuid,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        CommentResponse response = interactionService.addReply(commentId, userUuid, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @GetMapping("/comments/{commentId}/replies")
    public ResponseEntity<ApiResponse<Page<CommentResponse>>> getReplies(
            @PathVariable Long commentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                interactionService.getReplies(commentId, PageRequest.of(page, size))));
    }

    // --- Likes ---

    @PostMapping("/{uuid}/like")
    public ResponseEntity<ApiResponse<Void>> likeVideo(
            @PathVariable String uuid,
            @RequestHeader("X-User-UUID") String userUuid
    ) {
        interactionService.likeVideo(uuid, userUuid);
        return ResponseEntity.ok(ApiResponse.success("Liked"));
    }

    @DeleteMapping("/{uuid}/like")
    public ResponseEntity<ApiResponse<Void>> unlikeVideo(
            @PathVariable String uuid,
            @RequestHeader("X-User-UUID") String userUuid
    ) {
        interactionService.unlikeVideo(uuid, userUuid);
        return ResponseEntity.ok(ApiResponse.success("Unliked"));
    }

    @GetMapping("/{uuid}/liked")
    public ResponseEntity<ApiResponse<Boolean>> hasLiked(
            @PathVariable String uuid,
            @RequestHeader(value = "X-User-UUID", required = false) String userUuid
    ) {
        if (userUuid == null) {
            return ResponseEntity.ok(ApiResponse.success(false));
        }
        return ResponseEntity.ok(ApiResponse.success(interactionService.hasLiked(uuid, userUuid)));
    }

    // --- Downloads ---

    @PostMapping("/{uuid}/download")
    public ResponseEntity<ApiResponse<DownloadResponse>> requestDownload(
            @PathVariable String uuid,
            @RequestParam("quality") String quality,
            @RequestHeader(value = "X-User-UUID", required = false) String userUuid
    ) {
        if (userUuid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }
        if (!downloadService.isProUser(userUuid)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Pro subscription required for downloads"));
        }
        VideoQuality videoQuality = VideoQuality.valueOf(quality.toUpperCase());
        DownloadResponse response = downloadService.generateDownloadToken(uuid, userUuid, videoQuality);
        return ResponseEntity.ok(ApiResponse.success(response, "Download token generated"));
    }

    @GetMapping("/download/{token}")
    public ResponseEntity<org.springframework.core.io.InputStreamResource> resolveDownload(@PathVariable String token) {
        VideoDownloadService.DownloadFile file = downloadService.resolveDownload(token);
        InputStream stream = downloadService.getFileStream(file.mp4FileKey());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp4"))
                .header("Content-Disposition", "attachment; filename=\"" + file.filename() + "\"")
                .header("Content-Length", String.valueOf(file.fileSizeBytes()))
                .body(new org.springframework.core.io.InputStreamResource(stream));
    }

    // --- Views ---

    @PostMapping("/{uuid}/view")
    public ResponseEntity<ApiResponse<Void>> recordView(
            @PathVariable String uuid,
            @RequestHeader(value = "X-User-UUID", required = false) String userUuid,
            @RequestParam(value = "watchDuration", required = false) Integer watchDurationSeconds
    ) {
        interactionService.recordView(uuid, userUuid, watchDurationSeconds);
        return ResponseEntity.ok(ApiResponse.success("View recorded"));
    }
}
