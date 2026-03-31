package com.reveria.socialservice.repository;

import com.reveria.socialservice.model.entity.Post;
import com.reveria.socialservice.model.enums.PostVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    Optional<Post> findByUuid(String uuid);

    Page<Post> findByAuthorUuidAndVisibility(String authorUuid, PostVisibility visibility, Pageable pageable);

    Page<Post> findByAuthorUuid(String authorUuid, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE " +
           "((p.visibility = 'PUBLIC' OR " +
           "(p.visibility = 'FRIENDS_ONLY' AND p.authorUuid IN :friendUuids) OR " +
           "p.authorUuid = :userUuid)) AND " +
           "p.authorUuid NOT IN :blockedUuids")
    Page<Post> findFeedPosts(@Param("userUuid") String userUuid,
                             @Param("friendUuids") List<String> friendUuids,
                             @Param("blockedUuids") List<String> blockedUuids,
                             Pageable pageable);

    Page<Post> findByVisibility(PostVisibility visibility, Pageable pageable);
}
