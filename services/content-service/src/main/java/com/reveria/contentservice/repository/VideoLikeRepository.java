package com.reveria.contentservice.repository;

import com.reveria.contentservice.model.entity.VideoLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VideoLikeRepository extends JpaRepository<VideoLike, Long> {

    Optional<VideoLike> findByVideoIdAndUserUuid(Long videoId, String userUuid);

    boolean existsByVideoIdAndUserUuid(Long videoId, String userUuid);

    long countByVideoId(Long videoId);
}
