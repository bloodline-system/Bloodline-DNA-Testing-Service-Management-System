package com.dna_testing_system.dev.enums;

public enum ResultStatus {
    PENDING("Chưa xử lý"),
    PROCESSING("Đang xử lý"),
    COMPLETED("Đã hoàn thành"),
    INVALID("Không hợp lệ"),
    CANCELLED("Hủy kết quả"),
    DELIVERED("Đã gửi kết quả");

    private final String description;

    ResultStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
