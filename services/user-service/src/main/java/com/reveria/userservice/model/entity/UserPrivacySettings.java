package com.reveria.userservice.model.entity;

import com.reveria.userservice.model.enums.MessagePrivacy;
import com.reveria.userservice.model.enums.ProfileVisibility;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_privacy_settings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPrivacySettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ProfileVisibility profileVisibility = ProfileVisibility.PUBLIC;

    @Builder.Default
    private Boolean showOnlineStatus = true;

    @Builder.Default
    private Boolean allowDirectMessages = true;

    @Builder.Default
    private Boolean allowFriendRequests = true;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MessagePrivacy messagePrivacy = MessagePrivacy.EVERYONE;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}