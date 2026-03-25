package com.reveria.contentservice.controller;

import com.reveria.contentservice.dto.request.CreateChannelRequest;
import com.reveria.contentservice.dto.request.UpdateChannelRequest;
import com.reveria.contentservice.dto.response.ApiResponse;
import com.reveria.contentservice.dto.response.ChannelResponse;
import com.reveria.contentservice.service.ChannelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/content/channels")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;

    @PostMapping
    public ResponseEntity<ApiResponse<ChannelResponse>> createChannel(
            @RequestHeader("X-User-UUID") String userUuid,
            @Valid @RequestBody CreateChannelRequest request
    ) {
        ChannelResponse response = channelService.createChannel(userUuid, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Channel created"));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<ChannelResponse>> getChannel(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponse.success(channelService.getChannel(uuid)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<ChannelResponse>>> getMyChannels(
            @RequestHeader("X-User-UUID") String userUuid
    ) {
        return ResponseEntity.ok(ApiResponse.success(channelService.listMyChannels(userUuid)));
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<ApiResponse<ChannelResponse>> updateChannel(
            @PathVariable String uuid,
            @RequestHeader("X-User-UUID") String userUuid,
            @Valid @RequestBody UpdateChannelRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                channelService.updateChannel(uuid, userUuid, request)));
    }

    @PostMapping(value = "/{uuid}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ChannelResponse>> uploadAvatar(
            @PathVariable String uuid,
            @RequestHeader("X-User-UUID") String userUuid,
            @RequestParam("file") MultipartFile file
    ) {
        ChannelResponse response = channelService.uploadImage(uuid, userUuid, file, "avatar");
        return ResponseEntity.ok(ApiResponse.success(response, "Avatar updated"));
    }

    @PostMapping(value = "/{uuid}/banner", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ChannelResponse>> uploadBanner(
            @PathVariable String uuid,
            @RequestHeader("X-User-UUID") String userUuid,
            @RequestParam("file") MultipartFile file
    ) {
        ChannelResponse response = channelService.uploadImage(uuid, userUuid, file, "banner");
        return ResponseEntity.ok(ApiResponse.success(response, "Banner updated"));
    }

    @PostMapping("/{uuid}/regenerate-stream-key")
    public ResponseEntity<ApiResponse<ChannelResponse>> regenerateStreamKey(
            @PathVariable String uuid,
            @RequestHeader("X-User-UUID") String userUuid
    ) {
        ChannelResponse response = channelService.regenerateStreamKey(uuid, userUuid);
        return ResponseEntity.ok(ApiResponse.success(response, "Stream key regenerated"));
    }

    @PostMapping("/{uuid}/subscribe")
    public ResponseEntity<ApiResponse<Void>> subscribe(
            @PathVariable String uuid,
            @RequestHeader("X-User-UUID") String userUuid
    ) {
        channelService.subscribe(uuid, userUuid);
        return ResponseEntity.ok(ApiResponse.success("Subscribed"));
    }

    @DeleteMapping("/{uuid}/subscribe")
    public ResponseEntity<ApiResponse<Void>> unsubscribe(
            @PathVariable String uuid,
            @RequestHeader("X-User-UUID") String userUuid
    ) {
        channelService.unsubscribe(uuid, userUuid);
        return ResponseEntity.ok(ApiResponse.success("Unsubscribed"));
    }

    @GetMapping("/{uuid}/subscribed")
    public ResponseEntity<ApiResponse<Boolean>> isSubscribed(
            @PathVariable String uuid,
            @RequestHeader("X-User-UUID") String userUuid
    ) {
        return ResponseEntity.ok(ApiResponse.success(channelService.isSubscribed(uuid, userUuid)));
    }
}
