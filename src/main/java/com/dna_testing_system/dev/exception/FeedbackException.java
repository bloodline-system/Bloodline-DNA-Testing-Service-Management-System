package com.dna_testing_system.dev.exception;

public class FeedbackException extends ApplicationException {
    public FeedbackException(ErrorCode errorCode) {super(errorCode);}
    public FeedbackException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
