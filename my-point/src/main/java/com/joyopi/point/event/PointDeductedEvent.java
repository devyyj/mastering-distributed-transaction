package com.joyopi.point.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 포인트 차감 성공 이벤트
 * point-events 토픽으로 발행
 * 결제 서비스가 이 이벤트를 구독하여 결제를 진행한다.
 * usePoint는 결제 실패 시 PaymentFailedEvent에 담아 포인트 복원 요청에 사용한다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PointDeductedEvent {
    private Long orderId;
    private Long userId;
    private Long paymentAmount;
    private Long usePoint;
    private String idempotencyKey;
}
