package com.joyopi.point.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 포인트 차감/복구 이력 엔티티
 * 멱등성 검증을 위해 idempotencyKey에 UNIQUE 제약 조건을 설정합니다.
 */
@Entity
@Table(name = "point_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointHistoryType type;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    public enum PointHistoryType {
        DEDUCT, RESTORE
    }

    private PointHistory(Long userId, Long orderId, Long amount, PointHistoryType type, String idempotencyKey) {
        this.userId = userId;
        this.orderId = orderId;
        this.amount = amount;
        this.type = type;
        this.idempotencyKey = idempotencyKey;
    }

    public static PointHistory create(Long userId, Long orderId, Long amount, PointHistoryType type, String idempotencyKey) {
        return new PointHistory(userId, orderId, amount, type, idempotencyKey);
    }
}
