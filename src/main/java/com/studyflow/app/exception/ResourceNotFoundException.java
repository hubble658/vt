package com.studyflow.app.exception;

public class ResourceNotFoundException extends RuntimeException {
    private String argument;

    public ResourceNotFoundException(String argument) {
        this.argument = argument;
    }
}
