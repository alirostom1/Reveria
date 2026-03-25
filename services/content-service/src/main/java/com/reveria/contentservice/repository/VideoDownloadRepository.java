package com.reveria.contentservice.repository;

import com.reveria.contentservice.model.entity.VideoDownload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VideoDownloadRepository extends JpaRepository<VideoDownload, Long> {

    Optional<VideoDownload> findByDownloadToken(String downloadToken);
}
