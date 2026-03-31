package com.reveria.socialservice.service;

import com.reveria.socialservice.dto.response.StoryResponse;
import com.reveria.socialservice.exception.ForbiddenException;
import com.reveria.socialservice.exception.ResourceNotFoundException;
import com.reveria.socialservice.mapper.PostMapper;
import com.reveria.socialservice.model.entity.Story;
import com.reveria.socialservice.model.entity.StoryView;
import com.reveria.socialservice.model.enums.MediaType;
import com.reveria.socialservice.repository.FollowRepository;
import com.reveria.socialservice.repository.FriendshipRepository;
import com.reveria.socialservice.repository.StoryRepository;
import com.reveria.socialservice.repository.StoryViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoryService {

    private final StoryRepository storyRepository;
    private final StoryViewRepository storyViewRepository;
    private final FriendshipRepository friendshipRepository;
    private final FollowRepository followRepository;
    private final MinioStorageService storageService;
    private final PostMapper postMapper;

    @Transactional
    public StoryResponse createStory(String userUuid, MultipartFile file, Integer durationSeconds) {
        String key = storageService.upload("stories", file);
        MediaType mediaType = isImage(file) ? MediaType.IMAGE : MediaType.VIDEO;

        Story story = Story.builder()
                .authorUuid(userUuid)
                .mediaType(mediaType)
                .fileKey(key)
                .durationSeconds(durationSeconds)
                .build();

        story = storyRepository.save(story);
        return enrichStoryResponse(postMapper.toStoryResponse(story), story, userUuid);
    }

    public List<StoryResponse> getFeedStories(String userUuid) {
        List<String> friendUuids = friendshipRepository.findFriendUuids(userUuid);
        List<String> followingUuids = followRepository.findByFollowerUuid(userUuid,
                        org.springframework.data.domain.PageRequest.of(0, 1000))
                .map(f -> f.getFollowingUuid())
                .getContent();

        List<String> allUuids = new ArrayList<>(friendUuids);
        allUuids.addAll(followingUuids);
        allUuids.add(userUuid);
        List<String> uniqueUuids = allUuids.stream().distinct().collect(Collectors.toList());

        List<Story> stories = storyRepository
                .findByAuthorUuidInAndExpiresAtAfterOrderByCreatedAtDesc(uniqueUuids, LocalDateTime.now());

        return stories.stream()
                .map(s -> enrichStoryResponse(postMapper.toStoryResponse(s), s, userUuid))
                .collect(Collectors.toList());
    }

    public StoryResponse getStory(String uuid, String viewerUuid) {
        Story story = storyRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Story not found"));
        return enrichStoryResponse(postMapper.toStoryResponse(story), story, viewerUuid);
    }

    @Transactional
    public void markViewed(String uuid, String viewerUuid) {
        Story story = storyRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Story not found"));

        if (story.getAuthorUuid().equals(viewerUuid)) return;

        if (!storyViewRepository.existsByStoryIdAndViewerUuid(story.getId(), viewerUuid)) {
            StoryView view = StoryView.builder()
                    .story(story)
                    .viewerUuid(viewerUuid)
                    .build();
            storyViewRepository.save(view);
            story.setViewCount(story.getViewCount() + 1);
            storyRepository.save(story);
        }
    }

    @Transactional
    public void deleteStory(String uuid, String userUuid) {
        Story story = storyRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Story not found"));
        if (!story.getAuthorUuid().equals(userUuid)) {
            throw new ForbiddenException("You can only delete your own stories");
        }
        storageService.delete(story.getFileKey());
        storyRepository.delete(story);
    }

    private StoryResponse enrichStoryResponse(StoryResponse response, Story story, String viewerUuid) {
        response.setUrl(storageService.getPresignedUrl(story.getFileKey()));
        if (viewerUuid != null) {
            response.setViewedByMe(storyViewRepository.existsByStoryIdAndViewerUuid(story.getId(), viewerUuid));
        }
        return response;
    }

    private boolean isImage(MultipartFile file) {
        String ct = file.getContentType();
        return ct != null && ct.startsWith("image/");
    }
}
