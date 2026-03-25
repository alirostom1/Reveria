package com.reveria.contentservice.repository;

import com.reveria.contentservice.model.entity.VideoCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VideoCategoryRepository extends JpaRepository<VideoCategory, Long> {

    Optional<VideoCategory> findBySlug(String slug);

    List<VideoCategory> findAllByOrderByDisplayOrderAsc();
}
