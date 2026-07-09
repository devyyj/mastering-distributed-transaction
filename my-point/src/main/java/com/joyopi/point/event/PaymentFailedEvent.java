package com.joyopi.point.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제 실패 이벤트 (보상 트랜잭션 트리거)
 * payment-events 토픽으로 수신
 * 결제 실패 시 포인트 복원을 위해 사용
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFailedEvent {
    private Long orderId;
    private Long userId;
    private Long usePoint;
    private String reason;
    private String idempotencyKey;
}
