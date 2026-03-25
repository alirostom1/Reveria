package com.reveria.contentservice.model.entity;

import com.reveria.contentservice.model.enums.ChannelStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "channels")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Channel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private String uuid;

    @Column(nullable = false)
    private String ownerUuid;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    private String bannerUrl;

    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChannelStatus status;

    @Column(unique = true, nullable = false)
    private String streamKey;

    @OneToMany(mappedBy = "channel", fetch = FetchType.LAZY)
    private List<LiveStream> liveStreams;

    @Builder.Default
    @Column(nullable = false)
    private Long subscriberCount = 0L;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }
        if (streamKey == null) {
            streamKey = UUID.randomUUID().toString().replace("-", "");
        }
        if (status == null) {
            status = ChannelStatus.ACTIVE;
        }
    }
}
