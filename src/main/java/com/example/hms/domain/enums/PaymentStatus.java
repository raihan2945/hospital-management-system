package com.example.hms.domain.enums;

public enum PaymentStatus {
    UNPAID("Unpaid"), PARTIALLY_PAID("Partially paid"), PAID("Paid");

    private final String label;
    PaymentStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
