package com.reveria.socialservice.dto.response;

import com.reveria.socialservice.model.enums.MediaType;
import lombok.Data;

@Data
public class PostMediaResponse {
    private Long id;
    private MediaType mediaType;
    private String url;
    private Integer displayOrder;
}
