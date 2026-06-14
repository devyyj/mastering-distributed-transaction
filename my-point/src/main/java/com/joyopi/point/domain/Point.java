package com.joyopi.point.domain;

import com.joyopi.point.common.exception.BusinessException;
import com.joyopi.point.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 포인트 엔티티
 */
@Entity
@Table(name = "points")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Point {

    @Id
    private Long userId;

    @Column(nullable = false)
    private Long balance;

    private Point(Long userId, Long balance) {
        this.userId = userId;
        this.balance = balance;
    }

    /**
     * 신규 사용자 포인트 생성 (기본 10,000 포인트)
     */
    public static Point create(Long userId) {
        return new Point(userId, 10000L);
    }

    /**
     * 포인트 차감
     */
     public void use(Long amount) {
        if (amount == null || amount < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (this.balance < amount) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_POINT);
        }
        this.balance -= amount;
    }

    /**
     * 포인트 복구 (보상 트랜잭션용)
     */
    public void restore(Long amount) {
        if (amount == null || amount < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        this.balance += amount;
    }
}
