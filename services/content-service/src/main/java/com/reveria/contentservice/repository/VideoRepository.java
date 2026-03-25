package com.reveria.contentservice.repository;

import com.reveria.contentservice.model.entity.Video;
import com.reveria.contentservice.model.enums.VideoStatus;
import com.reveria.contentservice.model.enums.VideoVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface VideoRepository extends JpaRepository<Video, Long>, JpaSpecificationExecutor<Video> {

    Optional<Video> findByUuid(String uuid);

    Page<Video> findByStatusAndVisibilityOrderByCreatedAtDesc(
            VideoStatus status, VideoVisibility visibility, Pageable pageable);

    Page<Video> findByUserUuidOrderByCreatedAtDesc(String userUuid, Pageable pageable);

    Page<Video> findByChannelIdAndStatusOrderByCreatedAtDesc(
            Long channelId, VideoStatus status, Pageable pageable);

    @Modifying
    @Query("UPDATE Video v SET v.viewCount = v.viewCount + 1 WHERE v.id = :videoId")
    void incrementViewCount(Long videoId);

    @Modifying
    @Query("UPDATE Video v SET v.likeCount = v.likeCount + 1 WHERE v.id = :videoId")
    void incrementLikeCount(Long videoId);

    @Modifying
    @Query("UPDATE Video v SET v.likeCount = v.likeCount - 1 WHERE v.id = :videoId AND v.likeCount > 0")
    void decrementLikeCount(Long videoId);

    @Modifying
    @Query("UPDATE Video v SET v.commentCount = v.commentCount + 1 WHERE v.id = :videoId")
    void incrementCommentCount(Long videoId);

    @Modifying
    @Query("UPDATE Video v SET v.commentCount = v.commentCount - 1 WHERE v.id = :videoId AND v.commentCount > 0")
    void decrementCommentCount(Long videoId);
}
