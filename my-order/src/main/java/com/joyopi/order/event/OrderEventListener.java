package com.joyopi.order.event;

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
 * [이벤트 흐름]
 * 정상: payment-events(PaymentApprovedEvent) → 주문 COMPLETED 처리
 * 보상: payment-events(PaymentFailedEvent) → 주문 FAILED 처리
 *
 * [Kafka 메시지 처리 전략]
 * Consumer는 String으로 수신하고 ObjectMapper로 역직렬화합니다.
 * 이는 하나의 토픽에 여러 이벤트 타입이 존재할 때 발생하는 역직렬화 문제를 방지합니다.
 *
 * [처리 토픽]
 * - payment-events: PaymentApprovedEvent(결제 성공) vs PaymentFailedEvent(결제 실패)를
 *   'reason' 필드 존재 여부로 구분합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    /**
     * payment-events 토픽 리스너
     * - reason 필드 없음: PaymentApprovedEvent → 주문 COMPLETED 처리
     * - reason 필드 있음: PaymentFailedEvent → 주문 FAILED 처리
     */
    @Transactional
    @KafkaListener(topics = "payment-events", groupId = "order-service-group")
    public void handlePaymentEvent(String message) {
        try {
            // PaymentApprovedEvent와 PaymentFailedEvent 모두 orderId, reason 필드 구조가 동일
            // reason 필드 유무로 이벤트 타입 구분
            tools.jackson.databind.JsonNode node = objectMapper.readTree(message);
            Long orderId = node.has("orderId") ? node.get("orderId").asLong() : null;

            if (orderId == null) {
                log.warn("payment-events 메시지에 orderId가 없습니다. message: {}", message);
                return;
            }

            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. id: " + orderId));

            if (order.getStatus() == com.joyopi.order.domain.OrderStatus.COMPLETED || order.getStatus() == com.joyopi.order.domain.OrderStatus.FAILED) {
                log.info("OrderEventListener - 이미 최종 처리된 주문입니다. orderId: {}, status: {}", orderId, order.getStatus());
                return;
            }

            // reason 필드 존재 → 결제 실패 이벤트(PaymentFailedEvent)
            if (node.has("reason") && !node.get("reason").isNull()) {
                String reason = node.get("reason").asText();
                log.info("OrderEventListener - 결제 실패 이벤트 수신. orderId: {}, 사유: {}", orderId, reason);
                order.fail();
                log.info("주문 실패(FAILED) 처리 완료. orderId: {}", orderId);
            } else {
                // reason 필드 없음 → 결제 성공 이벤트(PaymentApprovedEvent)
                log.info("OrderEventListener - 결제 성공 이벤트 수신. orderId: {}", orderId);
                order.complete();
                log.info("주문 최종 완료(COMPLETED) 처리 성공. orderId: {}", orderId);
            }
        } catch (Exception e) {
            log.error("payment-events 메시지 처리 중 오류 발생. message: {}", message, e);
            throw new RuntimeException("주문 이벤트 처리 중 예외 발생: " + e.getMessage(), e);
        }
    }

    /**
     * point-events 토픽 리스너
     * - PointDeductionFailedEvent 수신 → 주문 FAILED 처리
     */
    @Transactional
    @KafkaListener(topics = "point-events", groupId = "order-service-group")
    public void handlePointEvent(String message) {
        try {
            tools.jackson.databind.JsonNode node = objectMapper.readTree(message);

            // PointDeductionFailedEvent는 reason 필드가 있고 userId가 없음
            if (node.has("reason") && !node.get("reason").isNull() && !node.has("userId")) {
                Long orderId = node.has("orderId") ? node.get("orderId").asLong() : null;
                if (orderId == null) {
                    log.warn("point-events(실패) 메시지에 orderId가 없습니다. message: {}", message);
                    return;
                }
                String reason = node.get("reason").asText();
                log.info("OrderEventListener - 포인트 차감 실패 이벤트 수신. orderId: {}, 사유: {}", orderId, reason);

                Order order = orderRepository.findById(orderId)
                        .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. id: " + orderId));
                
                if (order.getStatus() == com.joyopi.order.domain.OrderStatus.COMPLETED || order.getStatus() == com.joyopi.order.domain.OrderStatus.FAILED) {
                    log.info("OrderEventListener - 이미 최종 처리된 주문입니다(포인트 이벤트 무시). orderId: {}, status: {}", orderId, order.getStatus());
                    return;
                }

                order.fail();
                log.info("주문 실패(FAILED) 처리 완료. orderId: {}", orderId);
            }
        } catch (Exception e) {
            log.error("point-events 메시지 처리 중 오류 발생. message: {}", message, e);
            throw new RuntimeException("주문 이벤트(포인트 차감 실패) 처리 중 예외 발생: " + e.getMessage(), e);
        }
    }
}
