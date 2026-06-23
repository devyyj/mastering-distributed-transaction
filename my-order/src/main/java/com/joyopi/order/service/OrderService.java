package com.joyopi.order.service;

import tools.jackson.databind.ObjectMapper;
import com.joyopi.order.domain.Order;
import com.joyopi.order.domain.OutboxEvent;
import com.joyopi.order.repository.OrderRepository;
import com.joyopi.order.repository.OutboxRepository;
import com.joyopi.order.service.dto.OrderCommand;
import com.joyopi.order.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public Long order(OrderCommand command) {
        log.info("주문 프로세스(아웃박스 패턴) 시작 - userId: {}, productPrice: {}", command.getUserId(), command.getProductPrice());

        // 1. 주문 엔티티 생성 (초기 상태 PENDING)
        Order order = Order.create(command.getUserId(), command.getProductPrice(), command.getUsePoint());
        Order savedOrder = orderRepository.save(order);

        // 2. 주문 생성 이벤트 객체 정의
        OrderCreatedEvent event = new OrderCreatedEvent(
                savedOrder.getId(),
                savedOrder.getUserId(),
                savedOrder.getPaymentAmount(),
                savedOrder.getUsePoint()
        );

        // 3. 이벤트를 JSON으로 변환하여 Outbox 테이블에 저장 (로컬 트랜잭션 통합)
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = OutboxEvent.create(
                    "order",
                    String.valueOf(savedOrder.getId()),
                    "OrderCreatedEvent",
                    payload
            );
            outboxRepository.save(outboxEvent);
            log.info("주문 생성 이벤트를 Outbox 테이블에 저장 완료 - orderId: {}, outboxId: {}", savedOrder.getId(), outboxEvent.getId());
        } catch (Exception e) {
            log.error("Outbox 이벤트 생성 및 저장 실패 - orderId: {}", savedOrder.getId(), e);
            throw new RuntimeException("이벤트 발행을 위한 Outbox 저장 실패", e);
        }

        // Dual Write 문제 테스트를 위한 강제 예외 발생 코드
        // userId가 9999L인 경우 강제로 예외를 발생시켜 DB 트랜잭션은 롤백되나 
        // Kafka 이벤트는 이미 발행되는 현상(Dual Write 불일치)을 재현합니다.
        // 아웃박스 패턴 적용으로 이제는 DB 트랜잭션 롤백 시 Outbox 테이블 내용도 같이 롤백되므로 불일치가 발생하지 않습니다.
        if (command.getUserId() != null && command.getUserId().equals(9999L)) {
            log.warn("Dual Write 테스트를 위한 강제 예외 발생! userId: {}", command.getUserId());
            throw new RuntimeException("강제 예외 발생 - Dual Write 테스트용");
        }

        return savedOrder.getId();
    }
}
