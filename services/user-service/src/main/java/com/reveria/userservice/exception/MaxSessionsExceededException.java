package com.reveria.userservice.exception;

public class MaxSessionsExceededException extends RuntimeException {

    public MaxSessionsExceededException(String message) {
        super(message);
    }
}