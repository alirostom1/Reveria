package com.reveria.contentservice.controller;

import com.reveria.contentservice.dto.request.CreateLiveStreamRequest;
import com.reveria.contentservice.dto.response.ApiResponse;
import com.reveria.contentservice.dto.response.LiveStreamResponse;
import com.reveria.contentservice.service.LiveStreamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/content/streams")
@RequiredArgsConstructor
public class LiveStreamController {

    private final LiveStreamService liveStreamService;

    @PostMapping
    public ResponseEntity<ApiResponse<LiveStreamResponse>> goLive(
            @RequestHeader("X-User-UUID") String userUuid,
            @Valid @RequestBody CreateLiveStreamRequest request
    ) {
        LiveStreamResponse response = liveStreamService.goLive(userUuid, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Stream created"));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<LiveStreamResponse>> getStream(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponse.success(liveStreamService.getStream(uuid)));
    }

    @GetMapping("/active/{channelUuid}")
    public ResponseEntity<ApiResponse<LiveStreamResponse>> getActiveStream(@PathVariable String channelUuid) {
        LiveStreamResponse response = liveStreamService.getActiveStream(channelUuid);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/live")
    public ResponseEntity<ApiResponse<Page<LiveStreamResponse>>> listLiveStreams(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                liveStreamService.listLiveStreams(PageRequest.of(page, size))));
    }

    @GetMapping("/channel/{channelUuid}")
    public ResponseEntity<ApiResponse<Page<LiveStreamResponse>>> listChannelStreams(
            @PathVariable String channelUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                liveStreamService.listChannelStreams(channelUuid, PageRequest.of(page, size))));
    }

    @PostMapping("/{uuid}/end")
    public ResponseEntity<ApiResponse<LiveStreamResponse>> endStream(
            @PathVariable String uuid,
            @RequestHeader("X-User-UUID") String userUuid
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                liveStreamService.endStream(uuid, userUuid), "Stream ended"));
    }

    @PostMapping("/rtmp/on-publish")
    public ResponseEntity<Void> onPublish(@RequestParam("name") String streamKey) {
        boolean accepted = liveStreamService.onPublish(streamKey);
        if (accepted) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PostMapping("/rtmp/on-done")
    public ResponseEntity<Void> onDone(@RequestParam("name") String streamKey) {
        liveStreamService.onDone(streamKey);
        return ResponseEntity.ok().build();
    }
}
