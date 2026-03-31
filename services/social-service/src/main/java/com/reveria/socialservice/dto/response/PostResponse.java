package com.reveria.socialservice.dto.response;

import com.reveria.socialservice.model.enums.PostType;
import com.reveria.socialservice.model.enums.PostVisibility;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostResponse {
    private String uuid;
    private String authorUuid;
    private String content;
    private PostType type;
    private PostVisibility visibility;
    private String sharedVideoUuid;
    private Long likeCount;
    private Long commentCount;
    private List<PostMediaResponse> media;
    private boolean likedByMe;
    private LocalDateTime createdAt;
}
