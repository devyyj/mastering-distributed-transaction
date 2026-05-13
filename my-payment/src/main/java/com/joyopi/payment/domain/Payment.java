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
     * TCC - Try: 결제 예약 생성
     */
    public static Payment createReserved(Long orderId, Long amount) {
        return new Payment(orderId, amount, PaymentStatus.RESERVED);
    }

    /**
     * 실패한 결제 생성
     */
    public static Payment createFailed(Long orderId, Long amount) {
        return new Payment(orderId, amount, PaymentStatus.FAILED);
    }


    /**
     * TCC - Confirm: 결제 확정
     */
    public void confirm() {
        if (this.status != PaymentStatus.RESERVED) {
            throw new IllegalStateException("예약된 결제만 확정할 수 있습니다.");
        }
        this.status = PaymentStatus.COMPLETED;
    }

    /**
     * TCC - Cancel: 결제 취소
     */
    public void cancel() {
        if (this.status != PaymentStatus.RESERVED) {
            throw new IllegalStateException("예약된 결제만 취소할 수 있습니다.");
        }
        this.status = PaymentStatus.CANCELLED;
    }

    /**
     * 결제 실패 처리
     */
    public void fail() {
        this.status = PaymentStatus.FAILED;
    }

}
