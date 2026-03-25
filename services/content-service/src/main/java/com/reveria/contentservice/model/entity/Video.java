package com.reveria.contentservice.model.entity;

import com.reveria.contentservice.model.enums.VideoStatus;
import com.reveria.contentservice.model.enums.VideoVisibility;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "videos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private String uuid;

    @Column(nullable = false)
    private String userUuid;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    private String thumbnailUrl;

    private String originalFileKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VideoStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VideoVisibility visibility;

    private Long durationSeconds;

    @Builder.Default
    @Column(nullable = false)
    private Long viewCount = 0L;

    @Builder.Default
    @Column(nullable = false)
    private Long likeCount = 0L;

    @Builder.Default
    @Column(nullable = false)
    private Long commentCount = 0L;

    @Builder.Default
    @Column(nullable = false)
    private boolean allowComments = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean allowDownloads = true;

    private LocalDateTime publishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id")
    private Channel channel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private VideoCategory category;

    @ManyToMany
    @JoinTable(
            name = "video_tag_mappings",
            joinColumns = @JoinColumn(name = "video_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private Set<VideoTag> tags = new HashSet<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Transient
    public Duration getDuration() {
        return durationSeconds != null ? Duration.ofSeconds(durationSeconds) : null;
    }

    public void setDuration(Duration duration) {
        this.durationSeconds = duration != null ? duration.getSeconds() : null;
    }

    @PrePersist
    public void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }
        if (status == null) {
            status = VideoStatus.PENDING;
        }
        if (visibility == null) {
            visibility = VideoVisibility.PUBLIC;
        }
    }
}
