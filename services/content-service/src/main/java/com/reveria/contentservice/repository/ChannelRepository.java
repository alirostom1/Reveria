package com.reveria.contentservice.repository;

import com.reveria.contentservice.model.entity.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ChannelRepository extends JpaRepository<Channel, Long> {

    Optional<Channel> findByUuid(String uuid);

    Optional<Channel> findByOwnerUuid(String ownerUuid);

    Optional<Channel> findByStreamKey(String streamKey);

    boolean existsByOwnerUuid(String ownerUuid);

    long countByOwnerUuid(String ownerUuid);

    java.util.List<Channel> findAllByOwnerUuid(String ownerUuid);

    @Modifying
    @Query("UPDATE Channel c SET c.subscriberCount = c.subscriberCount + 1 WHERE c.id = :channelId")
    void incrementSubscriberCount(Long channelId);

    @Modifying
    @Query("UPDATE Channel c SET c.subscriberCount = c.subscriberCount - 1 WHERE c.id = :channelId AND c.subscriberCount > 0")
    void decrementSubscriberCount(Long channelId);
}
