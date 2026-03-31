package com.reveria.socialservice.model.entity;

import com.reveria.socialservice.model.enums.MediaType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stories")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Story {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String uuid;

    @Column(name = "author_uuid", nullable = false)
    private String authorUuid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaType mediaType;

    @Column(nullable = false)
    private String fileKey;

    private Integer durationSeconds;

    @Column(nullable = false)
    @Builder.Default
    private Long viewCount = 0L;

    @OneToMany(mappedBy = "story", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StoryView> views = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (uuid == null) uuid = java.util.UUID.randomUUID().toString();
        if (expiresAt == null) expiresAt = createdAt.plusHours(24);
    }
}
