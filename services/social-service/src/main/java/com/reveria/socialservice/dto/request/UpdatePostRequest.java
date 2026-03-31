package com.reveria.socialservice.dto.request;

import com.reveria.socialservice.model.enums.PostVisibility;
import lombok.Data;

@Data
public class UpdatePostRequest {
    private String content;
    private PostVisibility visibility;
}
