package com.example.hms.dto;

import com.example.hms.domain.enums.PaymentMethod;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class PaymentForm {
    @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 11, fraction = 2)
    private BigDecimal amount;
    @NotNull(message = "Select a payment method.")
    private PaymentMethod paymentMethod;
    @Size(max = 100)
    private String transactionReference;
    @NotNull(message = "Reload the invoice before recording payment.") @PositiveOrZero
    private Long version;

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod method) { paymentMethod = method; }
    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String reference) { transactionReference = reference; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
