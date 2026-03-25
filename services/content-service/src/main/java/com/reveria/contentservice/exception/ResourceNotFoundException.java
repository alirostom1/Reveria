package com.reveria.contentservice.exception;

public class ResourceNotFoundException extends RuntimeException {

    private final String resourceType;

    public ResourceNotFoundException(String resourceType, String identifier) {
        super(resourceType + " not found: " + identifier);
        this.resourceType = resourceType;
    }

    public String getResourceType() {
        return resourceType;
    }
}
