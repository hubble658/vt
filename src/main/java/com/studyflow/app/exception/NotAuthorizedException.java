package com.studyflow.app.exception;

public class NotAuthorizedException extends RuntimeException {
    private String argument;

    public NotAuthorizedException(String argument) {
        this.argument = argument;
    }
}
