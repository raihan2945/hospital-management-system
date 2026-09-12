package com.example.hms.service;

import com.example.hms.domain.Bill;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class StandardBillingCalculator implements BillingCalculator {
    @Override
    public BigDecimal calculateTotal(Bill bill) { return bill.getSubtotal().subtract(bill.getDiscount()); }
}
