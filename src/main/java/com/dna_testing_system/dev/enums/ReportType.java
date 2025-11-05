package com.dna_testing_system.dev.enums;

public enum ReportType {
    DAILY_ORDERS("Daily Orders"),
    MONTHLY_REVENUE("Monthly Revenue"),
    STAFF_PRODUCTIVITY("Staff Productivity"),
    CUSTOMER_SATISFACTION("Customer Satisfaction"),
    HEALTH_SYSTEM("Health System"),
    QUALITY_METRICS("Quality Metrics");

    private final String description;

    ReportType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
