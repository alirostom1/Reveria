package com.reveria.socialservice.service;

import com.reveria.socialservice.dto.request.CreateCommentRequest;
import com.reveria.socialservice.dto.request.CreatePostRequest;
import com.reveria.socialservice.dto.request.UpdatePostRequest;
import com.reveria.socialservice.dto.response.*;
import com.reveria.socialservice.exception.ForbiddenException;
import com.reveria.socialservice.exception.ResourceNotFoundException;
import com.reveria.socialservice.mapper.PostMapper;
import com.reveria.socialservice.model.entity.*;
import com.reveria.socialservice.model.enums.MediaType;
import com.reveria.socialservice.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostCommentRepository commentRepository;
    private final PostCommentReplyRepository replyRepository;
    private final PostLikeRepository likeRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserBlockRepository blockRepository;
    private final MinioStorageService storageService;
    private final PostMapper postMapper;

    @Transactional
    public PostResponse createPost(String userUuid, CreatePostRequest request, List<MultipartFile> files) {
        Post post = Post.builder()
                .authorUuid(userUuid)
                .content(request.getContent())
                .type(request.getType())
                .visibility(request.getVisibility())
                .sharedVideoUuid(request.getSharedVideoUuid())
                .build();

        post = postRepository.save(post);

        if (files != null && !files.isEmpty()) {
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                String key = storageService.upload("posts", file);
                MediaType mediaType = isImage(file) ? MediaType.IMAGE : MediaType.VIDEO;

                PostMedia media = PostMedia.builder()
                        .post(post)
                        .fileKey(key)
                        .mediaType(mediaType)
                        .displayOrder(i)
                        .build();
                post.getMedia().add(media);
            }
            post = postRepository.save(post);
        }

        return enrichPostResponse(postMapper.toResponse(post), userUuid);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getFeed(String userUuid, int page, int size) {
        List<String> friendUuids = friendshipRepository.findFriendUuids(userUuid);
        List<String> blockedUuids = getBlockedUserUuids(userUuid);
        List<String> friendFilter = friendUuids.isEmpty() ? List.of("__none__") : friendUuids;
        List<String> blockedFilter = blockedUuids.isEmpty() ? List.of("__none__") : blockedUuids;

        Page<Post> posts = postRepository.findFeedPosts(userUuid, friendFilter, blockedFilter,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        return posts.map(p -> enrichPostResponse(postMapper.toResponse(p), userUuid));
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getPublicFeed(int page, int size) {
        return postRepository.findByVisibility(
                        com.reveria.socialservice.model.enums.PostVisibility.PUBLIC,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(p -> enrichPostResponse(postMapper.toResponse(p), null));
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getUserPosts(String userUuid, String viewerUuid, int page, int size) {
        Page<Post> posts = postRepository.findByAuthorUuid(userUuid,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return posts.map(p -> enrichPostResponse(postMapper.toResponse(p), viewerUuid));
    }

    @Transactional(readOnly = true)
    public PostResponse getPost(String uuid, String viewerUuid) {
        Post post = postRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        return enrichPostResponse(postMapper.toResponse(post), viewerUuid);
    }

    @Transactional
    public PostResponse updatePost(String uuid, String userUuid, UpdatePostRequest request) {
        Post post = postRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        if (!post.getAuthorUuid().equals(userUuid)) {
            throw new ForbiddenException("You can only edit your own posts");
        }
        if (request.getContent() != null) post.setContent(request.getContent());
        if (request.getVisibility() != null) post.setVisibility(request.getVisibility());
        post = postRepository.save(post);
        return enrichPostResponse(postMapper.toResponse(post), userUuid);
    }

    @Transactional
    public void deletePost(String uuid, String userUuid) {
        Post post = postRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        if (!post.getAuthorUuid().equals(userUuid)) {
            throw new ForbiddenException("You can only delete your own posts");
        }
        post.getMedia().forEach(m -> storageService.delete(m.getFileKey()));
        postRepository.delete(post);
    }

    @Transactional
    public PostResponse toggleLike(String uuid, String userUuid) {
        Post post = postRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        var existing = likeRepository.findByPostIdAndUserUuid(post.getId(), userUuid);
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
        } else {
            PostLike like = PostLike.builder().post(post).userUuid(userUuid).build();
            likeRepository.save(like);
            post.setLikeCount(post.getLikeCount() + 1);
        }
        postRepository.save(post);
        return enrichPostResponse(postMapper.toResponse(post), userUuid);
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getComments(String uuid, int page, int size) {
        Post post = postRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        return commentRepository.findByPostId(post.getId(),
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(postMapper::toCommentResponse);
    }

    @Transactional
    public CommentResponse addComment(String uuid, String userUuid, CreateCommentRequest request) {
        Post post = postRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        PostComment comment = PostComment.builder()
                .post(post)
                .userUuid(userUuid)
                .content(request.getContent())
                .build();
        comment = commentRepository.save(comment);

        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);

        return postMapper.toCommentResponse(comment);
    }

    @Transactional
    public ReplyResponse addReply(Long commentId, String userUuid, CreateCommentRequest request) {
        PostComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        PostCommentReply reply = PostCommentReply.builder()
                .comment(comment)
                .userUuid(userUuid)
                .content(request.getContent())
                .build();
        reply = replyRepository.save(reply);
        return postMapper.toReplyResponse(reply);
    }

    @Transactional
    public void deleteComment(Long commentId, String userUuid) {
        PostComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        if (!comment.getUserUuid().equals(userUuid) && !comment.getPost().getAuthorUuid().equals(userUuid)) {
            throw new ForbiddenException("Not authorized to delete this comment");
        }
        Post post = comment.getPost();
        commentRepository.delete(comment);
        post.setCommentCount(Math.max(0, post.getCommentCount() - 1));
        postRepository.save(post);
    }

    private PostResponse enrichPostResponse(PostResponse response, String viewerUuid) {
        Post post = postRepository.findByUuid(response.getUuid()).orElse(null);
        if (post != null) {
            List<PostMediaResponse> mediaResponses = new ArrayList<>();
            for (PostMedia media : post.getMedia()) {
                PostMediaResponse mr = new PostMediaResponse();
                mr.setId(media.getId());
                mr.setMediaType(media.getMediaType());
                mr.setDisplayOrder(media.getDisplayOrder());
                mr.setUrl(storageService.getPresignedUrl(media.getFileKey()));
                mediaResponses.add(mr);
            }
            response.setMedia(mediaResponses);
            if (viewerUuid != null) {
                response.setLikedByMe(likeRepository.existsByPostIdAndUserUuid(post.getId(), viewerUuid));
            }
        }
        return response;
    }

    private boolean isImage(MultipartFile file) {
        String ct = file.getContentType();
        return ct != null && ct.startsWith("image/");
    }

    private List<String> getBlockedUserUuids(String userUuid) {
        return blockRepository.findByBlockerUuid(userUuid,
                        PageRequest.of(0, 1000))
                .map(b -> b.getBlockedUuid())
                .getContent();
    }
}
