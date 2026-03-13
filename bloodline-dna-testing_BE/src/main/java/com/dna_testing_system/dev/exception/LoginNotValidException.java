package com.dna_testing_system.dev.exception;

public class LoginNotValidException extends RuntimeException {
  public LoginNotValidException(String message) {
    super(message);
  }
}
