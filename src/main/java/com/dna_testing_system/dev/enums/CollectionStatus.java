package com.dna_testing_system.dev.enums;

public enum CollectionStatus {
    PENDING("Pending"),
    SCHEDULED("Scheduled"),
    COLLECTED("Collected"),
    CANCELLED("Cancelled"),
    FAILED("Failed");

    private final String description;

    CollectionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
