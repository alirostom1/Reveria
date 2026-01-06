package com.reveria.userservice.repository;

import com.reveria.userservice.model.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    long countByIdentifierAndSuccessfulFalseAndAttemptedAtAfter(
            String identifier,
            LocalDateTime after
    );
}