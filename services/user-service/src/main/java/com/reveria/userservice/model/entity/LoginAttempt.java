package com.reveria.userservice.model.entity;

import com.reveria.userservice.model.enums.AccountType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_attempts", indexes = {
        @Index(name = "idx_login_attempts_identifier", columnList = "identifier"),
        @Index(name = "idx_login_attempts_ip", columnList = "ipAddress"),
        @Index(name = "idx_login_attempts_created", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String identifier;

    @Column(nullable = false)
    private String ipAddress;

    private String userAgent;

    @Builder.Default
    private Boolean successful = false;

    private String failureReason;

    @Enumerated(EnumType.STRING)
    private AccountType accountType;

    @CreationTimestamp
    private LocalDateTime createdAt;
}