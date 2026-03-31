package com.reveria.socialservice.model.entity;

import com.reveria.socialservice.model.enums.MediaType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "post_media")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PostMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaType mediaType;

    @Column(nullable = false)
    private String fileKey;

    @Column(nullable = false)
    private Integer displayOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
}
