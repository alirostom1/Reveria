package com.reveria.contentservice.repository;

import com.reveria.contentservice.model.entity.VideoView;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoViewRepository extends JpaRepository<VideoView, Long> {

    long countByVideoId(Long videoId);
}
