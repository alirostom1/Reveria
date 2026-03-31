package com.reveria.socialservice.repository;

import com.reveria.socialservice.model.entity.Friendship;
import com.reveria.socialservice.model.enums.FriendshipStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    @Query("SELECT f FROM Friendship f WHERE " +
           "(f.userUuid = :userUuid OR f.friendUuid = :userUuid) AND f.status = :status")
    Page<Friendship> findByUserUuidOrFriendUuidAndStatus(@Param("userUuid") String userUuid,
                                                          @Param("status") FriendshipStatus status,
                                                          Pageable pageable);

    @Query("SELECT f FROM Friendship f WHERE f.friendUuid = :userUuid AND f.status = 'PENDING'")
    Page<Friendship> findPendingRequests(@Param("userUuid") String userUuid, Pageable pageable);

    @Query("SELECT CASE WHEN f.userUuid = :userUuid THEN f.friendUuid ELSE f.userUuid END " +
           "FROM Friendship f WHERE (f.userUuid = :userUuid OR f.friendUuid = :userUuid) AND f.status = 'ACCEPTED'")
    List<String> findFriendUuids(@Param("userUuid") String userUuid);

    @Query("SELECT f FROM Friendship f WHERE " +
           "((f.userUuid = :userUuid AND f.friendUuid = :friendUuid) OR " +
           "(f.userUuid = :friendUuid AND f.friendUuid = :userUuid))")
    Optional<Friendship> findBetween(@Param("userUuid") String userUuid, @Param("friendUuid") String friendUuid);
}
