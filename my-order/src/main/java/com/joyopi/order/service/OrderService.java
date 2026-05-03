package com.joyopi.order.service;

import com.joyopi.order.domain.Order;
import com.joyopi.order.repository.OrderRepository;
import com.joyopi.order.service.dto.OrderCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 주문 서비스
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;

    private static final String POINT_SERVICE_URL = "http://localhost:8082/api/points/use";
    private static final String PAYMENT_SERVICE_URL = "http://localhost:8083/api/payments";

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

        // 2. 포인트 차감 (my-point 호출)
        restTemplate.postForEntity(POINT_SERVICE_URL, 
                Map.of("userId", command.getUserId(), "amount", command.getUsePoint()), 
                Void.class);

        // 3. 결제 처리 (my-payment 호출)
        restTemplate.postForEntity(PAYMENT_SERVICE_URL, 
                Map.of("orderId", order.getId(), "amount", order.getPaymentAmount()), 
                Void.class);

        // 4. 주문 완료
        order.complete();

        return order.getId();
    }
}
