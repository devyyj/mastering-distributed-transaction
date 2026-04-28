package com.joyopi.monolith.payment.domain;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 결제 레포지토리
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
