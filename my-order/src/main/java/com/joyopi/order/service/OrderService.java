package com.joyopi.order.service;

import com.joyopi.order.domain.Order;
import com.joyopi.order.repository.OrderRepository;
import com.joyopi.order.service.dto.OrderCommand;
import com.joyopi.order.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
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
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public Long order(OrderCommand command) {
        log.info("주문 프로세스(코레오그래피) 시작 - userId: {}, productPrice: {}", command.getUserId(), command.getProductPrice());

        // 1. 주문 엔티티 생성 (초기 상태 PENDING)
        Order order = Order.create(command.getUserId(), command.getProductPrice(), command.getUsePoint());
        Order savedOrder = orderRepository.save(order);

        // 2. 주문 생성 이벤트 발행
        OrderCreatedEvent event = new OrderCreatedEvent(
                savedOrder.getId(),
                savedOrder.getUserId(),
                savedOrder.getPaymentAmount(),
                savedOrder.getUsePoint()
        );
        kafkaTemplate.send("order-events", event);
        log.info("주문 생성 이벤트 발행 완료 - orderId: {}", savedOrder.getId());

        return savedOrder.getId();
    }
}
