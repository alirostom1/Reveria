package com.reveria.contentservice.repository;

import com.reveria.contentservice.model.entity.Recommendation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    Page<Recommendation> findByUserUuidAndExpiresAtAfterOrderByScoreDesc(
            String userUuid, LocalDateTime now, Pageable pageable);

    void deleteByExpiresAtBefore(LocalDateTime now);
}
