package com.reveria.userservice.exception;

import lombok.Getter;

@Getter
public class FileValidationException extends RuntimeException {

    private final String field;

    public FileValidationException(String message, String field) {
        super(message);
        this.field = field;
    }
}
