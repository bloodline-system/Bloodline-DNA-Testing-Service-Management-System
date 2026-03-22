package com.dna_testing_system.dev.exception;

import lombok.Getter;

/**
 * Exception thrown when authentication fails.
 * This can be due to invalid credentials, expired tokens, or account lockouts.
 */
@Getter
public class AuthenticationException extends ApplicationException {
    public AuthenticationException(ErrorCode errorCode) {
        super(errorCode);
    }
}
