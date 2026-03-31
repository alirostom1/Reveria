package com.reveria.socialservice.repository;

import com.reveria.socialservice.model.entity.StoryView;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoryViewRepository extends JpaRepository<StoryView, Long> {

    boolean existsByStoryIdAndViewerUuid(Long storyId, String viewerUuid);
}
