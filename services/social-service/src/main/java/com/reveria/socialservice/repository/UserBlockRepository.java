package com.reveria.socialservice.repository;

import com.reveria.socialservice.model.entity.UserBlock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    Optional<UserBlock> findByBlockerUuidAndBlockedUuid(String blockerUuid, String blockedUuid);

    boolean existsByBlockerUuidAndBlockedUuid(String blockerUuid, String blockedUuid);

    Page<UserBlock> findByBlockerUuid(String blockerUuid, Pageable pageable);
}
