package com.dna_testing_system.dev.enums;

public enum ServiceCategory {
    CIVIL("Civil Service"),
    ADMINISTRATIVE("Administrative Service");

    private final String description;

    ServiceCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
