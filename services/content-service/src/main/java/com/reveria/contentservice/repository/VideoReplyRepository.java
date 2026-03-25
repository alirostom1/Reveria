package com.reveria.contentservice.repository;

import com.reveria.contentservice.model.entity.VideoReply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoReplyRepository extends JpaRepository<VideoReply, Long> {

    Page<VideoReply> findByCommentIdOrderByCreatedAtAsc(Long commentId, Pageable pageable);
}
