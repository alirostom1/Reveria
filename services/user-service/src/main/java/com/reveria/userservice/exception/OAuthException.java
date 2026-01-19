package com.reveria.userservice.exception;

import com.reveria.userservice.model.enums.ProviderType;
import lombok.Getter;

@Getter
public class OAuthException extends RuntimeException {

    private final ProviderType provider;

    public OAuthException(String message, ProviderType provider) {
        super(message);
        this.provider = provider;
    }

    public OAuthException(String message, ProviderType provider, Throwable cause) {
        super(message, cause);
        this.provider = provider;
    }
}