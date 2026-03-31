package com.reveria.socialservice.repository;

import com.reveria.socialservice.model.entity.PostComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostCommentRepository extends JpaRepository<PostComment, Long> {

    Page<PostComment> findByPostId(Long postId, Pageable pageable);

    long countByPostId(Long postId);
}
