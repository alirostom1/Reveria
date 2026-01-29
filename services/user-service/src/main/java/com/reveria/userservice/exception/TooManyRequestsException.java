package com.reveria.userservice.exception;

import lombok.Getter;

@Getter
public class TooManyRequestsException extends RuntimeException {

    private final int retryAfterMinutes;

    public TooManyRequestsException(int retryAfterMinutes) {
        super("Too many requests. Please try again later.");
        this.retryAfterMinutes = retryAfterMinutes;
    }
}