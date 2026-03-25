package com.reveria.contentservice.exception;

import lombok.Getter;

@Getter
public class VideoValidationException extends RuntimeException {

    private final String field;

    public VideoValidationException(String field, String message) {
        super(message);
        this.field = field;
    }
}
