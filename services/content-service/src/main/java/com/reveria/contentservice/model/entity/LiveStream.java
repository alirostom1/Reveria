package com.reveria.contentservice.model.entity;

import com.reveria.contentservice.model.enums.LiveStreamStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "live_streams")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveStream {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private String uuid;

    @Column(nullable = false)
    private String hostUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    private String hlsPlaybackUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LiveStreamStatus status;

    @Builder.Default
    @Column(nullable = false)
    private Long viewerCount = 0L;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    @PrePersist
    public void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }
        if (status == null) {
            status = LiveStreamStatus.IDLE;
        }
    }
}
