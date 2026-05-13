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

    private Order(Long userId, Long productPrice, Long usePoint) {
        if (productPrice < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        this.userId = userId;
        this.productPrice = productPrice;
        this.usePoint = usePoint;
        this.paymentAmount = productPrice - usePoint;
        this.status = OrderStatus.PENDING;
    }

    public static Order create(Long userId, Long productPrice, Long usePoint) {
        return new Order(userId, productPrice, usePoint);
    }

    public void reserve() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태에서만 예약할 수 있습니다.");
        }
        this.status = OrderStatus.RESERVED;
    }

    public void complete() {
        if (this.status != OrderStatus.RESERVED) {
            throw new IllegalStateException("RESERVED 상태에서만 확정할 수 있습니다.");
        }
        this.status = OrderStatus.COMPLETED;
    }

    public void cancel() {
        if (this.status != OrderStatus.RESERVED) {
            throw new IllegalStateException("RESERVED 상태에서만 취소할 수 있습니다.");
        }
        this.status = OrderStatus.CANCELLED;
    }

    public void fail() {
        this.status = OrderStatus.FAILED;
    }

}
