package com.joyopi.point.event;

import tools.jackson.databind.ObjectMapper;
import com.joyopi.point.service.PointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 포인트 서비스 이벤트 리스너
 *
 * [이벤트 흐름]
 * 정상: order-events(OrderCreatedEvent) → 포인트 차감 → PointDeductedEvent 적재 (Outbox)
 * 보상: payment-events(PaymentFailedEvent) → 포인트 복원 → PointRestoredEvent 적재 (Outbox)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointEventListener {

    private final PointService pointService;
    private final ObjectMapper objectMapper;

    /**
     * order-events 토픽 리스너 (정상 흐름 진입점)
     * OrderCreatedEvent 수신 → 포인트 차감 → Outbox에 PointDeductedEvent 적재
     * 포인트 부족 시 → Outbox에 PointDeductionFailedEvent 적재
     */
    @KafkaListener(topics = "order-events", groupId = "point-service-group")
    public void handleOrderCreated(String message) {
        try {
            OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);
            log.info("PointEventListener - 주문 생성 이벤트 수신. orderId: {}, userId: {}, 차감포인트: {}",
                    event.getOrderId(), event.getUserId(), event.getUsePoint());

            // 포인트 차감 및 Outbox 적재 (단일 로컬 트랜잭션)
            pointService.usePoint(event.getOrderId(), event.getUserId(), event.getUsePoint(), event.getPaymentAmount());
            log.info("포인트 차감 및 아웃박스 적재 성공. orderId: {}", event.getOrderId());

        } catch (Exception e) {
            log.error("order-events 메시지 처리 중 오류 발생. message: {}", message, e);
            try {
                OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);
                if (event.getOrderId() != null) {
                    // 독립된 트랜잭션으로 포인트 차감 실패 이벤트를 아웃박스에 적재
                    pointService.savePointDeductionFailedOutbox(event.getOrderId(), e.getMessage());
                }
            } catch (Exception ex) {
                log.error("포인트 차감 실패 아웃박스 적재 실패. message: {}", message, ex);
            }
        }
    }

    /**
     * payment-events 토픽 리스너 (보상 트랜잭션)
     * PaymentFailedEvent 수신 → 포인트 복원 → Outbox에 PointRestoredEvent 적재
     * userId, usePoint 필드가 없는 메시지(PaymentApprovedEvent)는 무시
     */
    @KafkaListener(topics = "payment-events", groupId = "point-service-compensate-group")
    public void handlePaymentFailed(String message) {
        try {
            PaymentFailedEvent event = objectMapper.readValue(message, PaymentFailedEvent.class);

            // reason, userId, 또는 usePoint가 null이면 PaymentFailedEvent가 아니거나 유효하지 않으므로 무시
            if (event.getReason() == null || event.getUserId() == null || event.getUsePoint() == null) {
                log.debug("PointEventListener - 유효하지 않은 PaymentFailedEvent 메시지 무시. orderId: {}",
                        event.getOrderId());
                return;
            }

            log.info("PointEventListener - 결제 실패 이벤트 수신. orderId: {}, userId: {}, 사유: {}",
                    event.getOrderId(), event.getUserId(), event.getReason());

            // 실습용 강제 예외 발생 코드 (userId가 9999L인 경우 보상 트랜잭션 실패 유도)
            if (Long.valueOf(9999L).equals(event.getUserId())) {
                log.warn("[실습] 강제 보상 트랜잭션 실패 예외를 발생시킵니다. userId: {}", event.getUserId());
                throw new RuntimeException("실습을 위한 강제 보상 트랜잭션 실패");
            }

            // 포인트 복원 및 Outbox 적재 (단일 로컬 트랜잭션)
            pointService.restorePoint(event.getOrderId(), event.getUserId(), event.getUsePoint());
            log.info("포인트 복원 및 아웃박스 적재 성공. orderId: {}", event.getOrderId());

        } catch (Exception e) {
            log.error("payment-events 메시지 처리 중 오류 발생. message: {}", message, e);
            throw new RuntimeException("보상 트랜잭션(포인트 복구) 처리 중 예외 발생: " + e.getMessage(), e);
        }
    }
}
