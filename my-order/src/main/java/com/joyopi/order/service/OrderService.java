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

    private static final String POINT_BASE_URL = "http://localhost:8082/api/points";
    private static final String PAYMENT_BASE_URL = "http://localhost:8083/api/payments";

    /**
     * TCC 기반 주문 프로세스 실행
     */
    @Transactional(noRollbackFor = Exception.class)
    public Long order(OrderCommand command) {
        log.info("TCC 주문 프로세스 시작 - userId: {}, productPrice: {}", command.getUserId(), command.getProductPrice());

        // 1. 주문 생성 (PENDING)
        Order order = orderRepository.save(Order.create(
                command.getUserId(),
                command.getProductPrice(),
                command.getUsePoint()
        ));

        boolean pointTrySuccess = false;
        boolean paymentTrySuccess = false;

        try {
            // [Try 단계]
            order.reserve(); // 주문 예약 (RESERVED)
            
            // 2. 포인트 예약 (my-point 호출)
            log.info("[Try] 포인트 예약 시도 - userId: {}, amount: {}", command.getUserId(), command.getUsePoint());
            restTemplate.postForEntity(POINT_BASE_URL + "/try", 
                    Map.of("userId", command.getUserId(), "amount", command.getUsePoint()), 
                    Void.class);
            pointTrySuccess = true;

            // 3. 결제 예약 (my-payment 호출)
            log.info("[Try] 결제 예약 시도 - orderId: {}, amount: {}", order.getId(), order.getPaymentAmount());
            restTemplate.postForEntity(PAYMENT_BASE_URL + "/try", 
                    Map.of("orderId", order.getId(), "amount", order.getPaymentAmount()), 
                    Void.class);
            paymentTrySuccess = true;

            // [Confirm 단계] - 모든 Try 성공 시
            log.info("[Confirm] 모든 리소스 확정 시도");
            
            // 4. 포인트 확정
            restTemplate.postForEntity(POINT_BASE_URL + "/confirm", 
                    Map.of("userId", command.getUserId(), "amount", command.getUsePoint()), 
                    Void.class);

            // 5. 결제 확정
            restTemplate.postForEntity(PAYMENT_BASE_URL + "/confirm", 
                    Map.of("orderId", order.getId()), 
                    Void.class);

            // 6. 주문 확정
            order.complete();
            log.info("TCC 주문 처리 성공 - orderId: {}", order.getId());

        } catch (Exception e) {
            log.error("TCC Try 단계 중 실패 발생, Cancel 단계 실행 - orderId: {}, 사유: {}", order.getId(), e.getMessage());
            
            // [Cancel 단계] - 하나라도 Try 실패 시
            try {
                if (paymentTrySuccess) {
                    log.info("[Cancel] 결제 예약 취소 시도 - orderId: {}", order.getId());
                    restTemplate.postForEntity(PAYMENT_BASE_URL + "/cancel", 
                            Map.of("orderId", order.getId()), 
                            Void.class);
                }
                if (pointTrySuccess) {
                    log.info("[Cancel] 포인트 예약 취소 시도 - userId: {}, amount: {}", command.getUserId(), command.getUsePoint());
                    restTemplate.postForEntity(POINT_BASE_URL + "/cancel", 
                            Map.of("userId", command.getUserId(), "amount", command.getUsePoint()), 
                            Void.class);
                }
            } catch (Exception cancelEx) {
                log.error("[Cancel] 취소 프로세스 중 추가 예외 발생 - orderId: {}", order.getId(), cancelEx);
                // 실제 운영 환경에서는 재시도 큐 등에 넣어야 함
            }

            order.cancel();
            throw e;
        }

        return order.getId();
    }

}
