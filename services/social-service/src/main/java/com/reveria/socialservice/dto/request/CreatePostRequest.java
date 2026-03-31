package com.reveria.socialservice.dto.request;

import com.reveria.socialservice.model.enums.PostType;
import com.reveria.socialservice.model.enums.PostVisibility;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreatePostRequest {
    private String content;

    @NotNull
    private PostType type;

    @NotNull
    private PostVisibility visibility;

    private String sharedVideoUuid;
}
