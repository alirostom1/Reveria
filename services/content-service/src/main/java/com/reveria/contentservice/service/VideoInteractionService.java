package com.reveria.contentservice.service;

import com.reveria.contentservice.dto.request.CreateCommentRequest;
import com.reveria.contentservice.dto.response.CommentResponse;
import com.reveria.contentservice.exception.ResourceNotFoundException;
import com.reveria.contentservice.model.entity.*;
import com.reveria.contentservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoInteractionService {

    private final VideoRepository videoRepository;
    private final VideoCommentRepository commentRepository;
    private final VideoReplyRepository replyRepository;
    private final VideoLikeRepository likeRepository;
    private final VideoViewRepository viewRepository;

    // --- Comments ---

    @Transactional
    public CommentResponse addComment(String videoUuid, String userUuid, CreateCommentRequest request) {
        Video video = findVideo(videoUuid);
        if (!video.isAllowComments()) {
            throw new IllegalStateException("Comments are disabled for this video");
        }

        VideoComment comment = VideoComment.builder()
                .videoId(video.getId())
                .userUuid(userUuid)
                .content(request.getContent())
                .build();
        comment = commentRepository.save(comment);
        videoRepository.incrementCommentCount(video.getId());

        return toCommentResponse(comment);
    }

    public Page<CommentResponse> getComments(String videoUuid, Pageable pageable) {
        Video video = findVideo(videoUuid);
        return commentRepository.findByVideoIdOrderByCreatedAtDesc(video.getId(), pageable)
                .map(this::toCommentResponse);
    }

    @Transactional
    public void deleteComment(Long commentId, String userUuid) {
        VideoComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", String.valueOf(commentId)));
        if (!comment.getUserUuid().equals(userUuid)) {
            throw new IllegalStateException("Not the comment author");
        }
        commentRepository.delete(comment);
        videoRepository.decrementCommentCount(comment.getVideoId());
    }

    // --- Replies ---

    @Transactional
    public CommentResponse addReply(Long commentId, String userUuid, CreateCommentRequest request) {
        if (!commentRepository.existsById(commentId)) {
            throw new ResourceNotFoundException("Comment", String.valueOf(commentId));
        }

        VideoReply reply = VideoReply.builder()
                .commentId(commentId)
                .userUuid(userUuid)
                .content(request.getContent())
                .build();
        reply = replyRepository.save(reply);

        return CommentResponse.builder()
                .id(reply.getId())
                .userUuid(reply.getUserUuid())
                .content(reply.getContent())
                .createdAt(formatDateTime(reply.getCreatedAt()))
                .build();
    }

    public Page<CommentResponse> getReplies(Long commentId, Pageable pageable) {
        return replyRepository.findByCommentIdOrderByCreatedAtAsc(commentId, pageable)
                .map(reply -> CommentResponse.builder()
                        .id(reply.getId())
                        .userUuid(reply.getUserUuid())
                        .content(reply.getContent())
                        .createdAt(formatDateTime(reply.getCreatedAt()))
                        .build());
    }

    // --- Likes ---

    @Transactional
    public void likeVideo(String videoUuid, String userUuid) {
        Video video = findVideo(videoUuid);
        if (likeRepository.existsByVideoIdAndUserUuid(video.getId(), userUuid)) {
            return;
        }

        VideoLike like = VideoLike.builder()
                .videoId(video.getId())
                .userUuid(userUuid)
                .build();
        likeRepository.save(like);
        videoRepository.incrementLikeCount(video.getId());
    }

    @Transactional
    public void unlikeVideo(String videoUuid, String userUuid) {
        Video video = findVideo(videoUuid);
        likeRepository.findByVideoIdAndUserUuid(video.getId(), userUuid)
                .ifPresent(like -> {
                    likeRepository.delete(like);
                    videoRepository.decrementLikeCount(video.getId());
                });
    }

    public boolean hasLiked(String videoUuid, String userUuid) {
        Video video = findVideo(videoUuid);
        return likeRepository.existsByVideoIdAndUserUuid(video.getId(), userUuid);
    }

    // --- Views ---

    @Transactional
    public void recordView(String videoUuid, String userUuid, Integer watchDurationSeconds) {
        Video video = findVideo(videoUuid);

        VideoView view = VideoView.builder()
                .videoId(video.getId())
                .userUuid(userUuid)
                .watchDurationSeconds(watchDurationSeconds)
                .build();
        viewRepository.save(view);
        videoRepository.incrementViewCount(video.getId());
    }

    // --- Helpers ---

    private Video findVideo(String uuid) {
        return videoRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Video", uuid));
    }

    private CommentResponse toCommentResponse(VideoComment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .userUuid(comment.getUserUuid())
                .content(comment.getContent())
                .createdAt(formatDateTime(comment.getCreatedAt()))
                .build();
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
