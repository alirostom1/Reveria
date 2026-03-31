package com.reveria.socialservice.repository;

import com.reveria.socialservice.model.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    Optional<PostLike> findByPostIdAndUserUuid(Long postId, String userUuid);

    boolean existsByPostIdAndUserUuid(Long postId, String userUuid);
}
