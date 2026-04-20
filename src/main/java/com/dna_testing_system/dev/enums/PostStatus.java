package com.dna_testing_system.dev.enums;

public enum PostStatus {
    DRAFT("Draft"),
    PUBLISHED("Published"),
    ARCHIVED("Archived"),
    DELETED("Deleted");

    private final String description;

    PostStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
