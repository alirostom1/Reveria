package com.reveria.userservice.model.entity;

import com.reveria.userservice.model.enums.ProviderType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "auth_providers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProviderType provider;

    @Column(nullable = false)
    private String providerId;

    private String accessToken;

    private String refreshToken;

    private LocalDateTime tokenExpiresAt;

    private LocalDateTime linkedAt;

    @PrePersist
    public void prePersist() {
        if (linkedAt == null) {
            linkedAt = LocalDateTime.now();
        }
    }
}