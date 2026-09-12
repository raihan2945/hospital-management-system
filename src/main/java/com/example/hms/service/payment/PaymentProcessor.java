package com.example.hms.service.payment;

import com.example.hms.domain.Bill;
import com.example.hms.domain.Payment;
import com.example.hms.domain.enums.PaymentMethod;
import java.math.BigDecimal;

/** Records an externally received payment. Implementations do not contact payment gateways. */
public interface PaymentProcessor {
    PaymentMethod method();
    Payment process(Bill bill, BigDecimal amount, String reference);
}
