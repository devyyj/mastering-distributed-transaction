package com.joyopi.payment.repository;

import com.joyopi.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 결제 레포지토리
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
