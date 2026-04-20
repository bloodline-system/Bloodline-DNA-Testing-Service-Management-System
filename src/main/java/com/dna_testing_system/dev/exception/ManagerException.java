package com.dna_testing_system.dev.exception;

import lombok.Getter;

@Getter
public class ManagerException extends ApplicationException {
    public ManagerException(ErrorCode errorCode) {
        super(errorCode);
    }
}