package com.reveria.userservice.exception;

import lombok.Getter;

@Getter
public class TokenReuseException extends RuntimeException {

    private final String familyId;

    public TokenReuseException(String familyId) {
        super("Token reuse detected for family: " + familyId);
        this.familyId = familyId;
    }
}
