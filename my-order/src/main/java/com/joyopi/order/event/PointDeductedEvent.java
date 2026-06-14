package com.joyopi.order.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 포인트 차감 성공 이벤트
 * point-events 토픽으로 발행됨
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PointDeductedEvent {
    private Long orderId;
}
