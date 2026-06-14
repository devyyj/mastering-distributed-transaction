package com.joyopi.point.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 포인트 복원 완료 이벤트 (보상 트랜잭션 완료 알림)
 * point-events 토픽으로 발행
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PointRestoredEvent {
    private Long orderId;
    private Long userId;
    private Long restoredAmount;
}
