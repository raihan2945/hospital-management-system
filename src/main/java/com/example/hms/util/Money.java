package com.example.hms.util;

import com.example.hms.exception.BillingValidationException;
import java.math.BigDecimal;

/** Decimal amounts are validated before storage; no silent rounding of staff input. */
public final class Money {
    private Money() { }
    public static BigDecimal nonNegative(BigDecimal value, String field) {
        if (value == null || value.signum() < 0 || value.scale() > 2
                || value.compareTo(new BigDecimal("99999999999.99")) > 0) {
            throw new BillingValidationException(field, "Enter a nonnegative amount with at most two decimal places.");
        }
        return value.setScale(2);
    }
}
