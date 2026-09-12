package com.example.hms.domain.enums;

public enum PaymentMethod {
    CASH("Cash"), CARD("Card"), MOBILE_BANKING("Mobile banking"), BANK_TRANSFER("Bank transfer");

    private final String label;
    PaymentMethod(String label) { this.label = label; }
    public String getLabel() { return label; }
}
