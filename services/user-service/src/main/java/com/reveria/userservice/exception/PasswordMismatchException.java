package com.reveria.userservice.exception;

public class PasswordMismatchException extends RuntimeException {

    public PasswordMismatchException() {
        super("Current password is incorrect");
    }
}