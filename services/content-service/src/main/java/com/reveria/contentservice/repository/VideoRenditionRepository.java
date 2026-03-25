package com.reveria.contentservice.repository;

import com.reveria.contentservice.model.entity.VideoRendition;
import com.reveria.contentservice.model.enums.VideoQuality;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VideoRenditionRepository extends JpaRepository<VideoRendition, Long> {

    List<VideoRendition> findByVideoIdOrderByQuality(Long videoId);

    Optional<VideoRendition> findByVideoIdAndQuality(Long videoId, VideoQuality quality);
}
