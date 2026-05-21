package com.joyopi.order.temporal.activity;

import com.joyopi.order.domain.Order;
import com.joyopi.order.repository.OrderRepository;
import com.joyopi.order.service.dto.OrderCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderActivityImpl implements OrderActivity {

    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public OrderResult createPendingOrder(OrderCommand command) {
        log.info("OrderActivity - 주문 생성 시작 (userId: {})", command.getUserId());
        Order order = Order.create(command.getUserId(), command.getProductPrice(), command.getUsePoint());
        order = orderRepository.save(order);
        
        return new OrderResult(
                order.getId(),
                order.getUserId(),
                order.getPaymentAmount(),
                order.getUsePoint()
        );
    }

    @Override
    @Transactional
    public void completeOrder(Long orderId) {
        log.info("OrderActivity - 주문 완료 처리 (orderId: {})", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId));
        order.complete();
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId) {
        log.info("OrderActivity - 주문 취소(보상) 처리 (orderId: {})", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId));
        order.fail();
    }
}
