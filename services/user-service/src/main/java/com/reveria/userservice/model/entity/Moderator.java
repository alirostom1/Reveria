package com.reveria.userservice.model.entity;

import com.reveria.userservice.model.enums.ModeratorRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "moderators")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Moderator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String uuid;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModeratorRole role;

    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime lastLoginAt;

    @PrePersist
    public void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }
    }
}