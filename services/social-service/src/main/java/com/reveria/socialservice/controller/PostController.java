package com.reveria.socialservice.controller;

import com.reveria.socialservice.dto.request.CreateCommentRequest;
import com.reveria.socialservice.dto.request.CreatePostRequest;
import com.reveria.socialservice.dto.request.UpdatePostRequest;
import com.reveria.socialservice.dto.response.*;
import com.reveria.socialservice.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/social/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @RequestHeader("X-User-UUID") String userUuid,
            @RequestPart("post") @Valid CreatePostRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        PostResponse response = postService.createPost(userUuid, request, files);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/feed")
    public ResponseEntity<ApiResponse<Page<PostResponse>>> getFeed(
            @RequestHeader("X-User-UUID") String userUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(postService.getFeed(userUuid, page, size)));
    }

    @GetMapping("/feed/public")
    public ResponseEntity<ApiResponse<Page<PostResponse>>> getPublicFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(postService.getPublicFeed(page, size)));
    }

    @GetMapping("/user/{userUuid}")
    public ResponseEntity<ApiResponse<Page<PostResponse>>> getUserPosts(
            @PathVariable String userUuid,
            @RequestHeader(value = "X-User-UUID", required = false) String viewerUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(postService.getUserPosts(userUuid, viewerUuid, page, size)));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<PostResponse>> getPost(
            @PathVariable String uuid,
            @RequestHeader(value = "X-User-UUID", required = false) String viewerUuid) {
        return ResponseEntity.ok(ApiResponse.ok(postService.getPost(uuid, viewerUuid)));
    }

    @PatchMapping("/{uuid}")
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @PathVariable String uuid,
            @RequestHeader("X-User-UUID") String userUuid,
            @RequestBody UpdatePostRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(postService.updatePost(uuid, userUuid, request)));
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable String uuid,
            @RequestHeader("X-User-UUID") String userUuid) {
        postService.deletePost(uuid, userUuid);
        return ResponseEntity.ok(ApiResponse.ok("Post deleted"));
    }

    @PostMapping("/{uuid}/like")
    public ResponseEntity<ApiResponse<PostResponse>> toggleLike(
            @PathVariable String uuid,
            @RequestHeader("X-User-UUID") String userUuid) {
        return ResponseEntity.ok(ApiResponse.ok(postService.toggleLike(uuid, userUuid)));
    }

    @GetMapping("/{uuid}/comments")
    public ResponseEntity<ApiResponse<Page<CommentResponse>>> getComments(
            @PathVariable String uuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(postService.getComments(uuid, page, size)));
    }

    @PostMapping("/{uuid}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @PathVariable String uuid,
            @RequestHeader("X-User-UUID") String userUuid,
            @RequestBody @Valid CreateCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(postService.addComment(uuid, userUuid, request)));
    }

    @PostMapping("/comments/{commentId}/replies")
    public ResponseEntity<ApiResponse<ReplyResponse>> addReply(
            @PathVariable Long commentId,
            @RequestHeader("X-User-UUID") String userUuid,
            @RequestBody @Valid CreateCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(postService.addReply(commentId, userUuid, request)));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long commentId,
            @RequestHeader("X-User-UUID") String userUuid) {
        postService.deleteComment(commentId, userUuid);
        return ResponseEntity.ok(ApiResponse.ok("Comment deleted"));
    }
}
