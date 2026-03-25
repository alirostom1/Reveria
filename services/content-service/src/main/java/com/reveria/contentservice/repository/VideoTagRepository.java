package com.reveria.contentservice.repository;

import com.reveria.contentservice.model.entity.VideoTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VideoTagRepository extends JpaRepository<VideoTag, Long> {

    Optional<VideoTag> findBySlug(String slug);

    Optional<VideoTag> findByNameIgnoreCase(String name);

    List<VideoTag> findTop20ByOrderByUsageCountDesc();
}
