package com.dna_testing_system.dev.exception;

public class SignUpNotValidException extends RuntimeException {
    public SignUpNotValidException(String message) {
        super(message);
    }
}
