package com.reveria.contentservice.model.entity;

import com.reveria.contentservice.model.enums.RenditionStatus;
import com.reveria.contentservice.model.enums.VideoQuality;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "video_renditions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"video_id", "quality"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoRendition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VideoQuality quality;

    private String hlsPlaylistKey;

    private String mp4FileKey;

    private Integer width;

    private Integer height;

    private Integer bitrate;

    private Long fileSizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RenditionStatus status;

    @PrePersist
    public void prePersist() {
        if (status == null) {
            status = RenditionStatus.PENDING;
        }
    }
}
