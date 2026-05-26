package com.joyopi.payment.temporal.activity;

import com.joyopi.payment.common.exception.BusinessException;
import com.joyopi.payment.service.PaymentService;
import io.temporal.failure.ApplicationFailure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentActivityImpl implements PaymentActivity {

    private final PaymentService paymentService;

    @Override
    public void processPayment(Long orderId, Long amount) {
        log.info("PaymentActivity - 결제 처리 시작 (orderId: {}, amount: {})", orderId, amount);
        try {
            paymentService.pay(orderId, amount);
        } catch (BusinessException e) {
            log.error("PaymentActivity - 결제 중 비즈니스 예외 발생 (재시도 중단): {}", e.getMessage());
            // 비즈니스 예외(한도 초과 등)는 아무리 재시도해도 다시 실패하므로 Non-Retryable ApplicationFailure로 감싸서 즉시 종료시킵니다.
            // Java 17+ 환경에서 Throwable(cause) 직렬화 시 모듈 시스템(Reflection) 제약으로 인한 직렬화 예외 방지를 위해 cause를 제외합니다.
            throw ApplicationFailure.newNonRetryableFailure(
                    e.getMessage(),
                    e.getClass().getName()
            );
        }
    }

    @Override
    public void cancelPayment(Long orderId, Long amount) {
        log.info("PaymentActivity - 결제 취소(보상) 시작 (orderId: {}, amount: {})", orderId, amount);
        paymentService.cancelPayment(orderId, amount);
    }
}
