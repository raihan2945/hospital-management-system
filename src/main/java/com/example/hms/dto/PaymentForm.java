package com.example.hms.dto;

import com.example.hms.domain.enums.PaymentMethod;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class PaymentForm {
    @NotNull(message = "Payment amount is required.")
    @DecimalMin(value = "0.00", inclusive = false, message = "Payment must be greater than zero.")
    @Digits(integer = 11, fraction = 2, message = "Payment must be at most 99999999999.99 with no more than two decimal places.")
    private BigDecimal amount;
    @NotNull(message = "Select a payment method.")
    private PaymentMethod paymentMethod;
    @Size(max = 100, message = "Transaction reference must not exceed 100 characters.")
    private String transactionReference;
    @NotNull(message = "Reload the invoice before recording payment.") @PositiveOrZero(message = "Reload the invoice before recording payment.")
    private Long version;

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod method) { paymentMethod = method; }
    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String reference) { transactionReference = reference == null || reference.isBlank() ? null : reference.strip(); }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
