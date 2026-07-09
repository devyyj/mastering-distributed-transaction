package com.joyopi.order.domain;

import com.joyopi.order.common.exception.BusinessException;
import com.joyopi.order.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 엔티티
 */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long productPrice;

    @Column(nullable = false)
    private Long usePoint;

    @Column(nullable = false)
    private Long paymentAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    private Order(Long userId, Long productPrice, Long usePoint, String idempotencyKey) {
        if (productPrice < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        this.userId = userId;
        this.productPrice = productPrice;
        this.usePoint = usePoint;
        this.paymentAmount = productPrice - usePoint;
        this.idempotencyKey = idempotencyKey;
        this.status = OrderStatus.PENDING;
    }

    public static Order create(Long userId, Long productPrice, Long usePoint, String idempotencyKey) {
        return new Order(userId, productPrice, usePoint, idempotencyKey);
    }

    public void complete() {
        this.status = OrderStatus.COMPLETED;
    }

    public void fail() {
        this.status = OrderStatus.FAILED;
    }
}
