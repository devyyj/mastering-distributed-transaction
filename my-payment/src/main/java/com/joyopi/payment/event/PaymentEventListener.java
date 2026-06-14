package com.joyopi.payment.event;

import tools.jackson.databind.ObjectMapper;
import com.joyopi.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 결제 서비스 이벤트 리스너
 *
 * [이벤트 흐름]
 * 정상: point-events(PointDeductedEvent) → 결제 처리 → payment-events(PaymentApprovedEvent)
 * 보상: 결제 실패 시 → payment-events(PaymentFailedEvent) 발행 (포인트 서비스가 복원 처리)
 *
 * [Kafka 메시지 처리 전략]
 * Consumer는 String으로 수신하고 ObjectMapper로 역직렬화합니다.
 * 이는 하나의 토픽에 여러 이벤트 타입이 존재할 때 발생하는 역직렬화 문제를 방지합니다.
 *
 * [처리 토픽]
 * - point-events: PointDeductedEvent 수신 → 결제 처리 → PaymentApprovedEvent/PaymentFailedEvent 발행
 *   (PointDeductionFailedEvent, PointRestoredEvent 등 reason/restoredAmount 필드 있는 메시지 무시)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final PaymentService paymentService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * point-events 토픽 리스너 (정상 흐름)
     * PointDeductedEvent 수신 → 결제 처리 → PaymentApprovedEvent 발행
     * 결제 실패 시 → PaymentFailedEvent 발행 (포인트 서비스가 복원 처리)
     * userId 필드가 없는 메시지(PointDeductionFailedEvent 등)는 무시
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

            paymentService.pay(event.getOrderId(), event.getPaymentAmount());

            // 결제 성공 이벤트 발행 (주문 서비스가 주문 완료 처리)
            PaymentApprovedEvent approvedEvent = new PaymentApprovedEvent(
                    event.getOrderId(),
                    1L, // paymentId 임시값
                    event.getUserId(),
                    null  // usePoint는 결제 서비스 관심사 아님
            );
            kafkaTemplate.send("payment-events", approvedEvent);
            log.info("결제 성공 이벤트 발행 완료. orderId: {}", event.getOrderId());

        } catch (Exception e) {
            log.error("point-events 메시지 처리 중 오류 발생. message: {}", message, e);
            try {
                PointDeductedEvent event = objectMapper.readValue(message, PointDeductedEvent.class);
                if (event.getOrderId() != null) {
                    // 결제 실패 이벤트 발행 (포인트 복원 보상 트랜잭션 트리거)
                    PaymentFailedEvent failedEvent = new PaymentFailedEvent(
                            event.getOrderId(),
                            event.getUserId(),
                            event.getUsePoint(),
                            e.getMessage()
                    );
                    kafkaTemplate.send("payment-events", failedEvent);
                    log.info("결제 실패 이벤트 발행 완료. orderId: {}", event.getOrderId());
                }
            } catch (Exception ex) {
                log.error("결제 실패 이벤트 발행 실패. message: {}", message, ex);
                throw new RuntimeException("결제 실패 이벤트 발행 실패: " + ex.getMessage(), ex);
            }
        }
    }
}
