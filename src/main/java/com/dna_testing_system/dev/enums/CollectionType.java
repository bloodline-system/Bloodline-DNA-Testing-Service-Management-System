package com.dna_testing_system.dev.enums;

public enum CollectionType {
    HOME_COLLECTION("Home Collection"),
    LAB_VISIT("Lab Visit");

    private final String description;

    CollectionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
