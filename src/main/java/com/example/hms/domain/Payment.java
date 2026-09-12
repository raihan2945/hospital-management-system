package com.example.hms.domain;

import com.example.hms.domain.enums.PaymentMethod;
import com.example.hms.exception.BillingValidationException;
import com.example.hms.util.Money;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "payments", indexes = @Index(name = "idx_payment_bill", columnList = "bill_id"))
public class Payment extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod;
    @Column(length = 100)
    private String transactionReference;
    @Column(nullable = false)
    private Instant paymentDate;

    protected Payment() { }
    public Payment(Bill bill, BigDecimal amount, PaymentMethod method, String reference) {
        this.bill = Objects.requireNonNull(bill);
        this.amount = Money.nonNegative(amount, "amount");
        if (this.amount.signum() == 0) { throw new BillingValidationException("amount", "Payment must be greater than zero."); }
        paymentMethod = Objects.requireNonNull(method);
        transactionReference = Person.optionalText(reference, "Transaction reference", 100);
        paymentDate = Instant.now();
    }
    public Bill getBill() { return bill; }
    public BigDecimal getAmount() { return amount; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public String getTransactionReference() { return transactionReference; }
    public Instant getPaymentDate() { return paymentDate; }
}
