package com.dna_testing_system.dev.enums;

public enum NotificationType {
    ORDER_UPDATE("Cập nhật đơn hàng"),
    RESULT_AVAILABLE("Có kết quả xét nghiệm"),
    PROMOTION("Khuyến mãi"),
    ACCOUNT_ACTIVITY("Hoạt động tài khoản"),
    SYSTEM_ANNOUNCEMENT("Thông báo hệ thống");

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
