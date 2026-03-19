package com.dna_testing_system.dev.exception;

public class PaymentException extends ApplicationException {
    public PaymentException(ErrorCode errorCode) {
        super(errorCode);
    }
}
