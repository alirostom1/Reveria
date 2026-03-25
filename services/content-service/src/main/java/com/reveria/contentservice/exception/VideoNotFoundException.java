package com.reveria.contentservice.exception;

public class VideoNotFoundException extends RuntimeException {

    public VideoNotFoundException(String uuid) {
        super("Video not found: " + uuid);
    }
}
