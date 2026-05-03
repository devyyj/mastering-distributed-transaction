package com.joyopi.monolith.order.service;

import com.joyopi.monolith.order.domain.Order;
import com.joyopi.monolith.order.repository.OrderRepository;
import com.joyopi.monolith.order.service.dto.OrderCommand;
import com.joyopi.monolith.payment.service.PaymentService;
import com.joyopi.monolith.point.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 서비스
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final PointService pointService;
    private final PaymentService paymentService;

    /**
     * 주문 생성 및 결제 프로세스 실행
     */
    @Transactional
    public Long order(OrderCommand command) {
        // 1. 주문 생성 (PENDING)
        Order order = orderRepository.save(Order.create(
                command.getUserId(),
                command.getProductPrice(),
                command.getUsePoint()
        ));

        // 2. 포인트 차감
        pointService.usePoint(command.getUserId(), command.getUsePoint());

        // 3. 결제 처리
        paymentService.pay(order.getId(), order.getPaymentAmount());

        // 4. 주문 완료
        order.complete();

        return order.getId();
    }
}
