package com.reveria.contentservice.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "video_views")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "video_id", nullable = false)
    private Long videoId;

    @Column(name = "user_uuid")
    private String userUuid;

    private Integer watchDurationSeconds;

    @Builder.Default
    private LocalDateTime viewedAt = LocalDateTime.now();
}
