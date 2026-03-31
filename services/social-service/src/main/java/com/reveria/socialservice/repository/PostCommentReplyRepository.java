package com.reveria.socialservice.repository;

import com.reveria.socialservice.model.entity.PostCommentReply;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostCommentReplyRepository extends JpaRepository<PostCommentReply, Long> {
}
