package com.dna_testing_system.dev.enums;

public enum SampleQuality {
    EXCELLENT("Excellent"),
    GOOD("Good"),
    ACCEPTABLE("Acceptable"),
    POOR("Poor");

    private final String description;

    SampleQuality(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
