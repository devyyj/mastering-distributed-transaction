package com.joyopi.order.service;

import com.joyopi.order.domain.Order;
import com.joyopi.order.repository.OrderRepository;
import com.joyopi.order.service.dto.OrderCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 주문 서비스
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;

    private static final String POINT_SERVICE_URL = "http://localhost:8082/api/points/use";
    private static final String POINT_RESTORE_URL = "http://localhost:8082/api/points/restore";
    private static final String PAYMENT_SERVICE_URL = "http://localhost:8083/api/payments";

    /**
     * 주문 생성 및 결제 프로세스 실행
     * RuntimeException 발생 시에도 롤백하지 않고 상태를 저장하기 위해 noRollbackFor 설정
     */
    @Transactional(noRollbackFor = Exception.class)
    public Long order(OrderCommand command) {
        log.info("주문 프로세스 시작 - userId: {}, productPrice: {}", command.getUserId(), command.getProductPrice());

        // 1. 주문 생성 (PENDING)
        Order order = orderRepository.save(Order.create(
                command.getUserId(),
                command.getProductPrice(),
                command.getUsePoint()
        ));

        try {
            // 2. 포인트 차감 (my-point 호출)
            log.info("포인트 차감 시도 - userId: {}, amount: {}", command.getUserId(), command.getUsePoint());
            restTemplate.postForEntity(POINT_SERVICE_URL, 
                    Map.of("userId", command.getUserId(), "amount", command.getUsePoint()), 
                    Void.class);

            try {
                // 3. 결제 처리 (my-payment 호출)
                log.info("결제 처리 시도 - orderId: {}, amount: {}", order.getId(), order.getPaymentAmount());
                restTemplate.postForEntity(PAYMENT_SERVICE_URL, 
                        Map.of("orderId", order.getId(), "amount", order.getPaymentAmount()), 
                        Void.class);
            } catch (Exception e) {
                // 결제 실패 시 포인트 복구 (보상 트랜잭션)
                log.error("결제 처리 실패 - orderId: {}, 사유: {}", order.getId(), e.getMessage());
                log.info("보상 트랜잭션: 포인트 복구 시도 - userId: {}, amount: {}", command.getUserId(), command.getUsePoint());
                restTemplate.postForEntity(POINT_RESTORE_URL, 
                        Map.of("userId", command.getUserId(), "amount", command.getUsePoint()), 
                        Void.class);
                throw e;
            }

            // 4. 주문 완료
            order.complete();
            log.info("주문 처리 성공 - orderId: {}", order.getId());

        } catch (Exception e) {
            log.error("주문 프로세스 중 예외 발생, 주문 실패 처리 - orderId: {}", order.getId());
            order.fail();
            // noRollbackFor 설정으로 인해 fail() 상태가 DB에 커밋됨
            throw e;
        }

        return order.getId();
    }
}
