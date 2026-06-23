package com.joyopi.payment.event;

import tools.jackson.databind.ObjectMapper;
import com.joyopi.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 결제 서비스 이벤트 리스너
 *
 * [이벤트 흐름]
 * 정상: point-events(PointDeductedEvent) → 결제 처리 → PaymentApprovedEvent 적재 (Outbox)
 * 보상: 결제 실패 시 → PaymentFailedEvent 적재 (Outbox) (포인트 서비스가 복원 처리)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    /**
     * point-events 토픽 리스너 (정상 흐름)
     * PointDeductedEvent 수신 → 결제 처리 및 Outbox에 PaymentApprovedEvent 적재
     * 결제 실패 시 → Outbox에 PaymentFailedEvent 적재 (포인트 복원 보상 트랜잭션 트리거)
     */
    @KafkaListener(topics = "point-events", groupId = "payment-service-group")
    public void handlePointDeducted(String message) {
        try {
            PointDeductedEvent event = objectMapper.readValue(message, PointDeductedEvent.class);

            // userId 또는 paymentAmount가 null이면 PointDeductedEvent가 아닌 다른 이벤트이므로 무시
            if (event.getUserId() == null || event.getPaymentAmount() == null) {
                log.debug("PaymentEventListener - 유효하지 않은 PointDeductedEvent 메시지 무시. orderId: {}", event.getOrderId());
                return;
            }

            log.info("PaymentEventListener - 포인트 차감 완료 이벤트 수신. orderId: {}, userId: {}, 결제금액: {}",
                    event.getOrderId(), event.getUserId(), event.getPaymentAmount());

            // 결제 처리 및 아웃박스 적재 (로컬 트랜잭션 통합)
            paymentService.pay(event.getOrderId(), event.getUserId(), event.getPaymentAmount(), event.getUsePoint());
            log.info("결제 성공 및 아웃박스 적재 완료. orderId: {}", event.getOrderId());

        } catch (Exception e) {
            log.error("point-events 메시지 처리 중 오류 발생. message: {}", message, e);
            try {
                PointDeductedEvent event = objectMapper.readValue(message, PointDeductedEvent.class);
                if (event.getOrderId() != null) {
                    // 독립 트랜잭션으로 결제 실패 이벤트를 아웃박스에 저장
                    paymentService.savePaymentFailedOutbox(
                            event.getOrderId(),
                            event.getUserId(),
                            event.getUsePoint(),
                            e.getMessage()
                    );
                }
            } catch (Exception ex) {
                log.error("결제 실패 아웃박스 적재 실패. message: {}", message, ex);
            }
        }
    }
}
