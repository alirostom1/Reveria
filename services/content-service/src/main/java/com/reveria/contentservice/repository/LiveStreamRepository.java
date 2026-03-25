package com.reveria.contentservice.repository;

import com.reveria.contentservice.model.entity.LiveStream;
import com.reveria.contentservice.model.enums.LiveStreamStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LiveStreamRepository extends JpaRepository<LiveStream, Long> {

    Optional<LiveStream> findByUuid(String uuid);

    Optional<LiveStream> findByChannelIdAndStatus(Long channelId, LiveStreamStatus status);

    List<LiveStream> findByChannelIdAndStatusIn(Long channelId, List<LiveStreamStatus> statuses);

    Page<LiveStream> findByStatusOrderByStartedAtDesc(LiveStreamStatus status, Pageable pageable);

    Page<LiveStream> findByHostUuidOrderByStartedAtDesc(String hostUuid, Pageable pageable);

    Page<LiveStream> findByChannelIdAndStatusOrderByStartedAtDesc(
            Long channelId, LiveStreamStatus status, Pageable pageable);
}
