package com.reveria.contentservice.repository;

import com.reveria.contentservice.model.entity.ChannelSubscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChannelSubscriptionRepository extends JpaRepository<ChannelSubscription, Long> {

    Optional<ChannelSubscription> findByChannelIdAndSubscriberUuid(Long channelId, String subscriberUuid);

    boolean existsByChannelIdAndSubscriberUuid(Long channelId, String subscriberUuid);

    Page<ChannelSubscription> findBySubscriberUuidOrderBySubscribedAtDesc(String subscriberUuid, Pageable pageable);

    Page<ChannelSubscription> findByChannelIdOrderBySubscribedAtDesc(Long channelId, Pageable pageable);

    long countByChannelId(Long channelId);
}
