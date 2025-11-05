package com.dna_testing_system.dev.enums;

public enum ServiceOrderStatus {
    PENDING("Pending"),
    CONFIRMED("Confirmed"),
    KIT_PREPARED("Kit Prepared"),
    KIT_SENT("Kit Sent"),
    SAMPLE_COLLECTED("Sample Collected"),
    SAMPLE_RECEIVED("Sample Received"),
    IN_PROGRESS("In Progress"),
    RESULT_AVAILABLE("Result Available"),
    IN_REVIEW("In Review"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String description;

    ServiceOrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
