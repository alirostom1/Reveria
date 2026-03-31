package com.reveria.socialservice.controller;

import com.reveria.socialservice.dto.response.ApiResponse;
import com.reveria.socialservice.dto.response.FollowResponse;
import com.reveria.socialservice.dto.response.FriendshipResponse;
import com.reveria.socialservice.dto.response.RelationshipStatusResponse;
import com.reveria.socialservice.service.RelationshipService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/social/relationships")
@RequiredArgsConstructor
public class RelationshipController {

    private final RelationshipService relationshipService;

    // ── Status ──

    @GetMapping("/status/{targetUuid}")
    public ResponseEntity<ApiResponse<RelationshipStatusResponse>> getRelationshipStatus(
            @RequestHeader("X-User-UUID") String userUuid,
            @PathVariable String targetUuid) {
        return ResponseEntity.ok(ApiResponse.ok(relationshipService.getRelationshipStatus(userUuid, targetUuid)));
    }

    // ── Friendships ──

    @PostMapping("/friends/request/{friendUuid}")
    public ResponseEntity<ApiResponse<FriendshipResponse>> sendFriendRequest(
            @RequestHeader("X-User-UUID") String userUuid,
            @PathVariable String friendUuid) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(relationshipService.sendFriendRequest(userUuid, friendUuid)));
    }

    @PutMapping("/friends/{id}/accept")
    public ResponseEntity<ApiResponse<FriendshipResponse>> acceptFriendRequest(
            @PathVariable Long id,
            @RequestHeader("X-User-UUID") String userUuid) {
        return ResponseEntity.ok(ApiResponse.ok(relationshipService.acceptFriendRequest(id, userUuid)));
    }

    @PutMapping("/friends/{id}/decline")
    public ResponseEntity<ApiResponse<FriendshipResponse>> declineFriendRequest(
            @PathVariable Long id,
            @RequestHeader("X-User-UUID") String userUuid) {
        return ResponseEntity.ok(ApiResponse.ok(relationshipService.declineFriendRequest(id, userUuid)));
    }

    @DeleteMapping("/friends/{id}")
    public ResponseEntity<ApiResponse<Void>> removeFriend(
            @PathVariable Long id,
            @RequestHeader("X-User-UUID") String userUuid) {
        relationshipService.removeFriend(id, userUuid);
        return ResponseEntity.ok(ApiResponse.ok("Friend removed"));
    }

    @GetMapping("/friends")
    public ResponseEntity<ApiResponse<Page<FriendshipResponse>>> getFriends(
            @RequestHeader("X-User-UUID") String userUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(relationshipService.getFriends(userUuid, page, size)));
    }

    @GetMapping("/friends/pending")
    public ResponseEntity<ApiResponse<Page<FriendshipResponse>>> getPendingRequests(
            @RequestHeader("X-User-UUID") String userUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(relationshipService.getPendingRequests(userUuid, page, size)));
    }

    // ── Follows ──

    @PostMapping("/follow/{targetUserUuid}")
    public ResponseEntity<ApiResponse<FollowResponse>> follow(
            @RequestHeader("X-User-UUID") String userUuid,
            @PathVariable String targetUserUuid) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(relationshipService.follow(userUuid, targetUserUuid)));
    }

    @DeleteMapping("/follow/{targetUserUuid}")
    public ResponseEntity<ApiResponse<Void>> unfollow(
            @RequestHeader("X-User-UUID") String userUuid,
            @PathVariable String targetUserUuid) {
        relationshipService.unfollow(userUuid, targetUserUuid);
        return ResponseEntity.ok(ApiResponse.ok("Unfollowed"));
    }

    @GetMapping("/followers")
    public ResponseEntity<ApiResponse<Page<FollowResponse>>> getFollowers(
            @RequestHeader("X-User-UUID") String userUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(relationshipService.getFollowers(userUuid, page, size)));
    }

    @GetMapping("/following")
    public ResponseEntity<ApiResponse<Page<FollowResponse>>> getFollowing(
            @RequestHeader("X-User-UUID") String userUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(relationshipService.getFollowing(userUuid, page, size)));
    }

    // ── Blocks ──

    @PostMapping("/block/{targetUserUuid}")
    public ResponseEntity<ApiResponse<Void>> blockUser(
            @RequestHeader("X-User-UUID") String userUuid,
            @PathVariable String targetUserUuid) {
        relationshipService.blockUser(userUuid, targetUserUuid);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("User blocked"));
    }

    @DeleteMapping("/block/{targetUserUuid}")
    public ResponseEntity<ApiResponse<Void>> unblockUser(
            @RequestHeader("X-User-UUID") String userUuid,
            @PathVariable String targetUserUuid) {
        relationshipService.unblockUser(userUuid, targetUserUuid);
        return ResponseEntity.ok(ApiResponse.ok("User unblocked"));
    }

    @GetMapping("/blocked")
    public ResponseEntity<ApiResponse<Page<String>>> getBlockedUsers(
            @RequestHeader("X-User-UUID") String userUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(relationshipService.getBlockedUsers(userUuid, page, size)));
    }
}
