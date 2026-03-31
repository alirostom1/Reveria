package com.reveria.socialservice.controller;

import com.reveria.socialservice.dto.response.ApiResponse;
import com.reveria.socialservice.dto.response.StoryResponse;
import com.reveria.socialservice.service.StoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/social/stories")
@RequiredArgsConstructor
public class StoryController {

    private final StoryService storyService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<StoryResponse>> createStory(
            @RequestHeader("X-User-UUID") String userUuid,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "durationSeconds", required = false) Integer durationSeconds) {
        StoryResponse response = storyService.createStory(userUuid, file, durationSeconds);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/feed")
    public ResponseEntity<ApiResponse<List<StoryResponse>>> getFeedStories(
            @RequestHeader("X-User-UUID") String userUuid) {
        return ResponseEntity.ok(ApiResponse.ok(storyService.getFeedStories(userUuid)));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<StoryResponse>> getStory(
            @PathVariable String uuid,
            @RequestHeader(value = "X-User-UUID", required = false) String viewerUuid) {
        return ResponseEntity.ok(ApiResponse.ok(storyService.getStory(uuid, viewerUuid)));
    }

    @PostMapping("/{uuid}/view")
    public ResponseEntity<ApiResponse<Void>> markViewed(
            @PathVariable String uuid,
            @RequestHeader("X-User-UUID") String viewerUuid) {
        storyService.markViewed(uuid, viewerUuid);
        return ResponseEntity.ok(ApiResponse.ok("Story marked as viewed"));
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<ApiResponse<Void>> deleteStory(
            @PathVariable String uuid,
            @RequestHeader("X-User-UUID") String userUuid) {
        storyService.deleteStory(uuid, userUuid);
        return ResponseEntity.ok(ApiResponse.ok("Story deleted"));
    }
}
