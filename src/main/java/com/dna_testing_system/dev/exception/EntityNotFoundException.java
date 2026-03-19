package com.dna_testing_system.dev.exception;

import lombok.Getter;

@Getter
public class EntityNotFoundException extends ApplicationException {
    public EntityNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
    public EntityNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode);
    }
}
