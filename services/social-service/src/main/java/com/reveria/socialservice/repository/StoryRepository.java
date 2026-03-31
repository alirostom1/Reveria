package com.reveria.socialservice.repository;

import com.reveria.socialservice.model.entity.Story;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StoryRepository extends JpaRepository<Story, Long> {

    Optional<Story> findByUuid(String uuid);

    List<Story> findByAuthorUuidInAndExpiresAtAfterOrderByCreatedAtDesc(List<String> authorUuids, LocalDateTime now);

    List<Story> findByAuthorUuidAndExpiresAtAfterOrderByCreatedAtDesc(String authorUuid, LocalDateTime now);
}
