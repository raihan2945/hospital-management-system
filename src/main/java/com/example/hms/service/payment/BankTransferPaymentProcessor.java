package com.example.hms.service.payment;

import com.example.hms.domain.*;
import com.example.hms.domain.enums.PaymentMethod;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class BankTransferPaymentProcessor implements PaymentProcessor {
    public PaymentMethod method() { return PaymentMethod.BANK_TRANSFER; }
    public Payment process(Bill bill, BigDecimal amount, String reference) {
        return new Payment(bill, amount, method(), reference);
    }
}
