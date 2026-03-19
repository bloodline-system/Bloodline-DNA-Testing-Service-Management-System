package com.dna_testing_system.dev.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class MedicalServiceException extends ApplicationException{
    public MedicalServiceException(ErrorCode errorCode) {
        super(errorCode);
    }
}
