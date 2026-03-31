package com.reveria.socialservice.repository;

import com.reveria.socialservice.model.entity.Follow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    Optional<Follow> findByFollowerUuidAndFollowingUuid(String followerUuid, String followingUuid);

    boolean existsByFollowerUuidAndFollowingUuid(String followerUuid, String followingUuid);

    Page<Follow> findByFollowingUuid(String followingUuid, Pageable pageable);

    Page<Follow> findByFollowerUuid(String followerUuid, Pageable pageable);

    long countByFollowingUuid(String followingUuid);

    long countByFollowerUuid(String followerUuid);

    List<String> findFollowingUuidByFollowerUuid(String followerUuid);
}
