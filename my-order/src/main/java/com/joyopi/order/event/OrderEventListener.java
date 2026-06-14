package com.joyopi.order.event;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.joyopi.order.domain.Order;
import com.joyopi.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 서비스 이벤트 리스너
 *
 * [Kafka 메시지 처리 전략]
 * Consumer는 String으로 수신하고 ObjectMapper로 역직렬화합니다.
 * 이는 하나의 토픽에 여러 이벤트 타입이 혼재할 때 발생하는 역직렬화 문제를 방지합니다.
 *
 * [토픽별 처리]
 * - point-events: PointDeductedEvent(차감 성공) vs PointDeductionFailedEvent(차감 실패)를
 *   'reason' 필드 존재 여부로 구분합니다.
 * - payment-events: PaymentCancelledEvent를 수신하여 주문을 FAILED로 처리합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    /**
     * point-events 토픽 리스너
     * - reason 필드 없음 → PointDeductedEvent → 주문 COMPLETED
     * - reason 필드 있음 → PointDeductionFailedEvent → 주문 FAILED
     */
    @Transactional
    @KafkaListener(topics = "point-events", groupId = "order-service-group")
    public void handlePointEvent(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            Long orderId = node.has("orderId") ? node.get("orderId").asLong() : null;

            if (orderId == null) {
                log.warn("point-events 메시지에 orderId가 없습니다. message: {}", message);
                return;
            }

            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. id: " + orderId));

            // reason 필드 존재 → 실패 이벤트
            if (node.has("reason") && !node.get("reason").isNull()) {
                String reason = node.get("reason").asText();
                log.info("OrderEventListener - 포인트 차감 실패 이벤트 수신. orderId: {}, 사유: {}", orderId, reason);
                order.fail();
                log.info("주문 실패(FAILED) 처리 완료. orderId: {}", orderId);
            } else {
                // reason 필드 없음 → 성공 이벤트
                log.info("OrderEventListener - 포인트 차감 성공 이벤트 수신. orderId: {}", orderId);
                order.complete();
                log.info("주문 최종 완료 처리 성공. orderId: {}", orderId);
            }
        } catch (Exception e) {
            log.error("point-events 메시지 처리 중 오류 발생. message: {}", message, e);
        }
    }

    /**
     * payment-events 토픽 리스너
     * PaymentCancelledEvent 수신 → 주문 FAILED 처리
     */
    @Transactional
    @KafkaListener(topics = "payment-events", groupId = "order-service-group-cancelled")
    public void handlePaymentCancelled(String message) {
        try {
            PaymentCancelledEvent event = objectMapper.readValue(message, PaymentCancelledEvent.class);
            if (event.getOrderId() == null) {
                log.warn("payment-events 메시지에 orderId가 없습니다. message: {}", message);
                return;
            }
            log.info("OrderEventListener - 결제 취소 이벤트 수신. orderId: {}, 사유: {}", event.getOrderId(), event.getReason());
            Order order = orderRepository.findById(event.getOrderId())
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. id: " + event.getOrderId()));
            order.fail();
            log.info("주문 실패(FAILED) 처리 완료 (결제 취소 유발). orderId: {}", order.getId());
        } catch (Exception e) {
            log.error("payment-events 메시지 처리 중 오류 발생. message: {}", message, e);
        }
    }
}
