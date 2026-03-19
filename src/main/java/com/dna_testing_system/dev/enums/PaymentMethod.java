package com.dna_testing_system.dev.enums;

public enum PaymentMethod {
    BANK_TRANSFER("Bank Transfer"),
    CASH("Cash"),
    CREDIT_CARD("Credit Card"),
    DEBIT_CARD("Debit Card"),
    MOMO("Momo"),
    VIETTEL_MONEY("Viettel money"),
    VNPAY("VNPay"),
    ZALO_PAY("Zalo pay");

    private final String description;

    PaymentMethod(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
