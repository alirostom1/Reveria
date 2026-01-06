package com.reveria.userservice.repository;

import com.reveria.userservice.model.entity.Moderator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ModeratorRepository extends JpaRepository<Moderator, Long> {

    Optional<Moderator> findByUuid(String uuid);

    Optional<Moderator> findByUsername(String username);

    boolean existsByUsername(String username);
}