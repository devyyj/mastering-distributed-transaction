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
 * [Kafka 메시지 처리 전략]
 * Consumer는 String으로 수신하고 ObjectMapper로 역직렬화합니다.
 * 이는 하나의 토픽에 여러 이벤트 타입이 혼재할 때 발생하는 역직렬화 문제를 방지합니다.
 *
 * [토픽별 처리]
 * - order-events: OrderCreatedEvent 수신 → 결제 처리 후 PaymentApprovedEvent/PaymentCancelledEvent 발행
 * - point-events: PointDeductionFailedEvent 수신 → 결제 취소 후 PaymentCancelledEvent 발행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final PaymentService paymentService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * order-events 토픽 리스너
     * OrderCreatedEvent 수신 → 결제 처리
     */
    @KafkaListener(topics = "order-events", groupId = "payment-service-group")
    public void handleOrderCreated(String message) {
        try {
            OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);
            log.info("PaymentEventListener - 주문 생성 이벤트 수신. orderId: {}, userId: {}, 결제금액: {}",
                    event.getOrderId(), event.getUserId(), event.getPaymentAmount());

            paymentService.pay(event.getOrderId(), event.getPaymentAmount());

            // 결제 성공 시 PaymentApprovedEvent 발행
            PaymentApprovedEvent approvedEvent = new PaymentApprovedEvent(
                    event.getOrderId(),
                    1L, // paymentId 예시
                    event.getUserId(),
                    event.getUsePoint()
            );
            kafkaTemplate.send("payment-events", approvedEvent);
            log.info("결제 성공 이벤트 발행 완료. orderId: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("주문 이벤트 처리 중 오류 발생. message: {}", message, e);
            try {
                OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);
                PaymentCancelledEvent cancelledEvent = new PaymentCancelledEvent(event.getOrderId(), e.getMessage());
                kafkaTemplate.send("payment-events", cancelledEvent);
            } catch (Exception ex) {
                log.error("결제 취소 이벤트 발행 실패. message: {}", message, ex);
            }
        }
    }

    /**
     * point-events 토픽 리스너
     * PointDeductionFailedEvent 수신 → 결제 취소 보상 트랜잭션
     * reason 필드가 있는 메시지만 처리 (PointDeductionFailedEvent 식별)
     */
    @KafkaListener(topics = "point-events", groupId = "payment-service-group-compensate")
    public void handlePointDeductionFailed(String message) {
        try {
            PointDeductionFailedEvent event = objectMapper.readValue(message, PointDeductionFailedEvent.class);

            // reason 필드가 없으면 PointDeductedEvent(성공) 이므로 무시
            if (event.getReason() == null) {
                return;
            }

            log.info("PaymentEventListener - 포인트 차감 실패 이벤트 수신. orderId: {}, 사유: {}",
                    event.getOrderId(), event.getReason());

            paymentService.cancelPayment(event.getOrderId(), 0L);

            PaymentCancelledEvent cancelledEvent = new PaymentCancelledEvent(event.getOrderId(), event.getReason());
            kafkaTemplate.send("payment-events", cancelledEvent);
            log.info("결제 취소 보상 이벤트 발행 완료. orderId: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("point-events 메시지 처리 중 오류 발생. message: {}", message, e);
        }
    }
}
