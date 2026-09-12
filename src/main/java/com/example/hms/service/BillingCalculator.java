package com.example.hms.service;

import com.example.hms.domain.Bill;
import java.math.BigDecimal;

public interface BillingCalculator {
    BigDecimal calculateTotal(Bill bill);
}
