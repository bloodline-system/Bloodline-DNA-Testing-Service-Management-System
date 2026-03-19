package com.dna_testing_system.dev.enums;

public enum PostCategory {
    DNA_KNOWLEDGE("DNA Knowledge"),
    TESTING_GUIDE("Testing Guide"),
    NEWS("News"),
    CASE_STUDIES("Case Studies"),
    ANNOUNCEMENTS("Announcements");

    private final String description;

    PostCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
