package com.reveria.userservice.repository;

import com.reveria.userservice.model.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(String token);

    Optional<EmailVerificationToken> findByUserIdAndUsedFalse(Long userId);

    boolean existsByUserIdAndUsedFalseAndExpiresAtAfter(Long userId, LocalDateTime now);

    @Modifying
    @Query("UPDATE EmailVerificationToken t SET t.used = true WHERE t.user.id = :userId AND t.used = false")
    void invalidateAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.used = true AND t.createdAt < :before")
    void deleteUsedTokensOlderThan(@Param("before") LocalDateTime before);
}