package com.dna_testing_system.dev.enums;

public enum PostTag {
    DNA_TESTING("DNA Testing"),
    HEALTH("Health"),
    FAMILY("Family"),
    LEGAL("Legal"),
    IMMIGRATION("Immigration"),
    CASE_STUDY("Case Study"),
    PROMOTION("Promotion"),
    NEWS("News"),
    GENETICS("Genetics"),
    DISEASE_SCREENING("Disease Screening"),
    TIPS("Tips"),
    CUSTOMER_STORY("Customer Story");

    private final String description;

    PostTag(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
