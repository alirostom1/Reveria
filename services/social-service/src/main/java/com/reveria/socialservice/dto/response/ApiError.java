package com.reveria.socialservice.dto.response;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ApiError {
    private String code;
    private String path;
}
