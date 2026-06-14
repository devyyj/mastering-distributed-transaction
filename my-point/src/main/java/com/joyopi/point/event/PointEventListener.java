package com.joyopi.point.event;

import tools.jackson.databind.ObjectMapper;
import com.joyopi.point.service.PointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 포인트 서비스 이벤트 리스너
 *
 * [Kafka 메시지 처리 전략]
 * Consumer는 String으로 수신하고 ObjectMapper로 역직렬화합니다.
 * 이는 하나의 토픽에 여러 이벤트 타입이 혼재할 때 발생하는 역직렬화 문제를 방지합니다.
 *
 * [토픽별 처리]
 * - payment-events: PaymentApprovedEvent 수신 → 포인트 차감 후 PointDeductedEvent/PointDeductionFailedEvent 발행
 *   (PaymentCancelledEvent는 userId, usePoint 없으므로 무시)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointEventListener {

    private final PointService pointService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * payment-events 토픽 리스너
     * - userId, usePoint 필드가 있는 메시지 → PaymentApprovedEvent → 포인트 차감 처리
     * - 해당 필드가 없으면 PaymentCancelledEvent이므로 무시
     */
    @KafkaListener(topics = "payment-events", groupId = "point-service-group")
    public void handlePaymentApproved(String message) {
        try {
            PaymentApprovedEvent event = objectMapper.readValue(message, PaymentApprovedEvent.class);

            // userId가 null이면 PaymentCancelledEvent이므로 무시
            if (event.getUserId() == null) {
                log.debug("PointEventListener - userId가 없는 메시지 무시 (PaymentCancelledEvent). orderId: {}", event.getOrderId());
                return;
            }

            log.info("PointEventListener - 결제 성공 이벤트 수신. orderId: {}, userId: {}, 차감포인트: {}",
                    event.getOrderId(), event.getUserId(), event.getUsePoint());

            // 포인트 차감
            pointService.usePoint(event.getUserId(), event.getUsePoint());

            // 포인트 차감 성공 이벤트 발행
            PointDeductedEvent deductedEvent = new PointDeductedEvent(event.getOrderId());
            kafkaTemplate.send("point-events", deductedEvent);
            log.info("포인트 차감 성공 이벤트 발행 완료. orderId: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("payment-events 메시지 처리 중 오류 발생. message: {}", message, e);
            try {
                PaymentApprovedEvent event = objectMapper.readValue(message, PaymentApprovedEvent.class);
                if (event.getOrderId() != null) {
                    PointDeductionFailedEvent failedEvent = new PointDeductionFailedEvent(event.getOrderId(), e.getMessage());
                    kafkaTemplate.send("point-events", failedEvent);
                    log.info("포인트 차감 실패 보상 이벤트 발행 완료. orderId: {}", event.getOrderId());
                }
            } catch (Exception ex) {
                log.error("포인트 차감 실패 이벤트 발행 실패. message: {}", message, ex);
            }
        }
    }
}
