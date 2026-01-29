package com.reveria.userservice.repository;

import com.reveria.userservice.model.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    @Query("SELECT COUNT(la) FROM LoginAttempt la " +
            "WHERE la.identifier = :identifier " +
            "AND la.successful = false " +
            "AND la.createdAt > :since")
    int countFailedAttemptsByIdentifier(
            @Param("identifier") String identifier,
            @Param("since") LocalDateTime since
    );

    @Query("SELECT COUNT(la) FROM LoginAttempt la " +
            "WHERE la.ipAddress = :ipAddress " +
            "AND la.successful = false " +
            "AND la.createdAt > :since")
    int countFailedAttemptsByIp(
            @Param("ipAddress") String ipAddress,
            @Param("since") LocalDateTime since
    );

    @Query("SELECT MAX(la.createdAt) FROM LoginAttempt la " +
            "WHERE la.identifier = :identifier " +
            "AND la.successful = false " +
            "AND la.createdAt > :since")
    Optional<LocalDateTime> findLastFailedAttemptTimeByIdentifier(
            @Param("identifier") String identifier,
            @Param("since") LocalDateTime since
    );


    @Query("SELECT MAX(la.createdAt) FROM LoginAttempt la " +
            "WHERE la.ipAddress = :ipAddress " +
            "AND la.successful = false " +
            "AND la.createdAt > :since")
    Optional<LocalDateTime> findLastFailedAttemptTimeByIp(
            @Param("ipAddress") String ipAddress,
            @Param("since") LocalDateTime since
    );

    @Modifying
    @Query("DELETE FROM LoginAttempt la WHERE la.createdAt < :before")
    void deleteOldAttempts(@Param("before") LocalDateTime before);
}
