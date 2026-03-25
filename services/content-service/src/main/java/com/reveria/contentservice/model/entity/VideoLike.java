package com.reveria.contentservice.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "video_likes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"video_id", "user_uuid"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "video_id", nullable = false)
    private Long videoId;

    @Column(name = "user_uuid", nullable = false)
    private String userUuid;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
