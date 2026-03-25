package com.reveria.contentservice.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "video_replies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "comment_id", nullable = false)
    private Long commentId;

    @Column(nullable = false)
    private String userUuid;

    @Column(nullable = false, length = 5000)
    private String content;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
