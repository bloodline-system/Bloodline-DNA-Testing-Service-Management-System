package com.dna_testing_system.dev.enums;

public enum KitStatus {
    ORDERED("Đã đặt"),
    PREPARED("Đã chuẩn bị"),
    SHIPPED("Đã gửi"),
    DELIVERED("Đã giao"),
    USED("Đã sử dụng");

    private final String description;

    KitStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
