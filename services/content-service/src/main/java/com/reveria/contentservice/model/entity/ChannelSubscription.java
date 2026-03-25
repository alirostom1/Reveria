package com.reveria.contentservice.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "channel_subscriptions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"channel_id", "subscriber_uuid"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "channel_id", nullable = false)
    private Long channelId;

    @Column(name = "subscriber_uuid", nullable = false)
    private String subscriberUuid;

    @Builder.Default
    private LocalDateTime subscribedAt = LocalDateTime.now();
}
