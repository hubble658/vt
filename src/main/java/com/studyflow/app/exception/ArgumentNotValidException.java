package com.studyflow.app.exception;

public class ArgumentNotValidException extends RuntimeException {
    public ArgumentNotValidException(String argument) {
        super(argument);
    }
}
