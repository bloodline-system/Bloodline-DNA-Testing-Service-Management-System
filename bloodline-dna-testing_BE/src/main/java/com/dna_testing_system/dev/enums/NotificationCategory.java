package com.dna_testing_system.dev.enums;

public enum NotificationCategory {
    EMAIL("Email"),
    SMS("Tin nhắn SMS"),
    PUSH("Thông báo đẩy"),
    IN_APP("Thông báo trong ứng dụng");

    private final String description;

    NotificationCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
