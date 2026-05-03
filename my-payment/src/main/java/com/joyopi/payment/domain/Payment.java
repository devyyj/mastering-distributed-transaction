package com.joyopi.payment.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제 엔티티
 */
@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private Payment(Long orderId, Long amount, PaymentStatus status) {
        this.orderId = orderId;
        this.amount = amount;
        this.status = status;
    }

    /**
     * 성공한 결제 생성
     */
    public static Payment createSuccess(Long orderId, Long amount) {
        return new Payment(orderId, amount, PaymentStatus.COMPLETED);
    }

    /**
     * 실패한 결제 생성
     */
    public static Payment createFailed(Long orderId, Long amount) {
        return new Payment(orderId, amount, PaymentStatus.FAILED);
    }
}
