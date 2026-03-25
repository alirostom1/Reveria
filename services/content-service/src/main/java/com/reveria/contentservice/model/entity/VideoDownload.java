package com.reveria.contentservice.model.entity;

import com.reveria.contentservice.model.enums.VideoQuality;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "video_downloads")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoDownload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "video_id", nullable = false)
    private Long videoId;

    @Column(nullable = false)
    private String userUuid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VideoQuality quality;

    @Column(unique = true, nullable = false)
    private String downloadToken;

    private LocalDateTime expiresAt;

    private LocalDateTime downloadedAt;

    @PrePersist
    public void prePersist() {
        if (downloadToken == null) {
            downloadToken = UUID.randomUUID().toString();
        }
    }
}
