package com.reveria.contentservice.repository;

import com.reveria.contentservice.model.entity.VideoComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoCommentRepository extends JpaRepository<VideoComment, Long> {

    Page<VideoComment> findByVideoIdOrderByCreatedAtDesc(Long videoId, Pageable pageable);

    long countByVideoId(Long videoId);
}
