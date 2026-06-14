package com.joyopi.order.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제 실패 이벤트
 * payment-events 토픽으로 수신
 * 결제 실패 시 주문을 FAILED 상태로 변경하는 데 사용
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
