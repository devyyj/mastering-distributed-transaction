package com.joyopi.payment.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제 실패 이벤트 (보상 트랜잭션 트리거)
 * payment-events 토픽으로 발행
 * 포인트 서비스가 이 이벤트를 구독하여 포인트를 복원한다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFailedEvent {
    private Long orderId;
    private Long userId;
    private Long usePoint;
    private String reason;
}
