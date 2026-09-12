package com.example.hms.repository;

import com.example.hms.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    @org.springframework.data.jpa.repository.Query("select sum(p.amount) from Payment p")
    java.math.BigDecimal sumCollectedAmount();

    List<Payment> findByBillIdOrderByPaymentDateAscIdAsc(Long billId);
}
