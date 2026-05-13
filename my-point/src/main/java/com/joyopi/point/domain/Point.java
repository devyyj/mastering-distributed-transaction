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

    @Column(nullable = false)
    private Long reservedPoint;

    private Point(Long userId, Long balance) {
        this.userId = userId;
        this.balance = balance;
        this.reservedPoint = 0L;
    }

    /**
     * 신규 사용자 포인트 생성 (기본 10,000 포인트)
     */
    public static Point create(Long userId) {
        return new Point(userId, 10000L);
    }

    /**
     * TCC - Try: 포인트 예약
     */
    public void tryUse(Long amount) {
        if (amount < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (this.balance < amount) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_POINT);
        }
        this.balance -= amount;
        this.reservedPoint += amount;
    }

    /**
     * TCC - Confirm: 포인트 사용 확정
     */
    public void confirmUse(Long amount) {
        if (this.reservedPoint < amount) {
            throw new BusinessException("예약된 포인트가 부족합니다.", ErrorCode.INVALID_INPUT_VALUE);
        }
        this.reservedPoint -= amount;
    }

    /**
     * TCC - Cancel: 포인트 예약 취소
     */
    public void cancelUse(Long amount) {
        if (this.reservedPoint < amount) {
            throw new BusinessException("예약된 포인트가 부족합니다.", ErrorCode.INVALID_INPUT_VALUE);
        }
        this.reservedPoint -= amount;
        this.balance += amount;
    }


    /**
     * 기존 use 메서드는 제거하거나 하위 호환을 위해 유지할 수 있으나 TCC로 교체하므로 제거/수정 고려
     * 여기서는 TCC로 완전히 전환한다고 가정하고 기존 use/restore는 제거합니다.
     */

}
