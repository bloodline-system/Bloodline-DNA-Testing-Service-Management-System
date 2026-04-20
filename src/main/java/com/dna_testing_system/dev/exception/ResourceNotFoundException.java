package com.dna_testing_system.dev.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public  ResourceNotFoundException(ErrorCode errorCode) {
        super(errorCode.getMessage());
    }
}
