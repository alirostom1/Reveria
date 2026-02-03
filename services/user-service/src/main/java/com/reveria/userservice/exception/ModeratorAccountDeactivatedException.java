package com.reveria.userservice.exception;

public class ModeratorAccountDeactivatedException extends RuntimeException {

    public ModeratorAccountDeactivatedException() {
        super("Moderator account is deactivated");
    }
}
