package com.dna_testing_system.dev.exception;


import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_CREDENTIALS("Invalid username or password"),
    USERNAME_INVALID( "Username must be least 3 characters"),
    NOT_FOUND( "Not Found"),
    USER_EXISTS("User is exists"),
    RESOURCE_NOT_FOUND("Resource not found"),
    EMAIL_EXISTS("Email is exists, please use other email address"),
    USER_NOT_EXISTS("User is not exists"),
    PASSWORD_INVALID("Password must be least 8 characters"),
    SERVICE_TYPE_EXISTS("Service type already exists"),
    SERVICE_TYPE_NOT_EXISTS("Service type is not exists"),
    SERVICE_FEATURE_NOT_EXISTS("Service feature is not exists"),
    MEDICAL_SERVICE_NOT_EXISTS("Medical service is not exists"),
    INVALID_STAFF_ASSIGNMENT("Staff assignment is invalid"),
    ASSIGNMENT_FAILED("Failed to complete dual task assignment"),
    INVALID_STATUS_TRANSITION("Invalid status transition"),
    MANIPULATION_SYSTEM_REPORT_FAILED("Operating system report failed"),
    VALIDATE_ENUMERATION_FAILED("Validation enum failed"),
    ORDER_NOT_EXISTS("Order is not exists"),
    FEEDBACK_PERSIST_ERROR("Feedback persistence error"),
    FEEDBACK_GETTING_ERROR("Feedback getting error"),
    FEEDBACK_RESPONDING_ERROR("Feedback responding error"),
    UNAUTHORIZED_FEEDBACK_ACTION("You do not have permission to modify or delete this feedback"),
    PAYMENT_NOT_EXISTS("Payment not exists"),
    PAYMENT_PERSIST_ERROR("Payment persistence error"),
    UNKNOWN_ERROR("Unknown error!");


    private String message;
    ErrorCode(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
