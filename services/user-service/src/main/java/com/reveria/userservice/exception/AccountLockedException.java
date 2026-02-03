package com.reveria.userservice.exception;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AccountLockedException extends RuntimeException {

    private final LocalDateTime lockedUntil;
    private final int remainingMinutes;

    public AccountLockedException(LocalDateTime lockedUntil) {
        super("Account is temporarily locked");
        this.lockedUntil = lockedUntil;
        this.remainingMinutes = (int) java.time.Duration.between(LocalDateTime.now(), lockedUntil).toMinutes();
    }
}